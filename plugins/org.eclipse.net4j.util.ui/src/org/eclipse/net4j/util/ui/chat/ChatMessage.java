/*
 * Copyright (c) 2024 Eike Stepper (Loehne, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.net4j.util.ui.chat;

import org.eclipse.net4j.internal.util.bundle.OM;
import org.eclipse.net4j.util.CheckUtil;
import org.eclipse.net4j.util.HexUtil;
import org.eclipse.net4j.util.StringUtil;
import org.eclipse.net4j.util.lifecycle.ILifecycle;
import org.eclipse.net4j.util.lifecycle.LifecycleEventAdapter;

import java.io.File;
import java.io.Serializable;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Eike Stepper
 * @since 3.19
 */
public interface ChatMessage extends Comparable<ChatMessage>
{
  public int getID();

  public Author getAuthor();

  public long getCreationTime();

  public long getEditTime();

  public String getContent();

  public ChatMessage getReplyTo();

  @Override
  public default int compareTo(ChatMessage o)
  {
    return Long.compare(getCreationTime(), o.getCreationTime());
  }

  /**
   * @author Eike Stepper
   */
  public static final class Author implements Serializable, Comparable<Author>
  {
    private static final long serialVersionUID = 1L;

    private final String userID;

    private final String firstName;

    private final String lastName;

    private final String fullName;

    private final String initials;

    // https://www.gravatar.com/avatar/
    private final URI avatar;

    private Author(String userID, String firstName, String lastName, URI avatar)
    {
      this.userID = userID;
      this.firstName = firstName;
      this.lastName = lastName;
      fullName = (firstName == null ? "" : firstName + " ") + lastName;
      initials = ((firstName == null ? "" : firstName.substring(0, 1)) + lastName.substring(0, 1)).toUpperCase();
      this.avatar = avatar;
    }

    public String getUserID()
    {
      return userID;
    }

    public String getFirstName()
    {
      return firstName;
    }

    public String getLastName()
    {
      return lastName;
    }

    public String getShortName()
    {
      return firstName == null ? lastName : firstName;
    }

    public String getFullName()
    {
      return fullName;
    }

    public String getInitials()
    {
      return initials;
    }

    public URI getAvatar()
    {
      return avatar;
    }

    @Override
    public int compareTo(Author o)
    {
      return userID.compareTo(o.userID);
    }

    @Override
    public int hashCode()
    {
      return Objects.hash(userID);
    }

    @Override
    public boolean equals(Object obj)
    {
      if (this == obj)
      {
        return true;
      }

      if (obj == null)
      {
        return false;
      }

      if (getClass() != obj.getClass())
      {
        return false;
      }

      Author other = (Author)obj;
      return Objects.equals(userID, other.userID);
    }

    @Override
    public String toString()
    {
      return "Author[" + userID + "]";
    }

    public static Builder builder(String userID)
    {
      return new Builder(userID);
    }

    /**
     * @author Eike Stepper
     */
    public static final class Builder
    {
      private static final MessageDigest SHA_256;

      private final String userID;

      private String firstName;

      private String lastName;

      // https://www.gravatar.com/avatar/
      private URI avatar;

      public Builder(String userID)
      {
        CheckUtil.checkArg(!StringUtil.isEmpty(userID), "userID");
        this.userID = userID;
      }

      public Builder firstName(String firstName)
      {
        this.firstName = firstName;
        return this;
      }

      public Builder lastName(String lastName)
      {
        this.lastName = lastName;
        return this;
      }

      public Builder avatar(URI avatar)
      {
        this.avatar = avatar;
        return this;
      }

      public Builder avatar(File avatar)
      {
        return avatar(avatar.toURI());
      }

      public Builder gravatar(String email)
      {
        CheckUtil.checkArg(!StringUtil.isEmpty(email), "email");
        return avatar(URI.create("https://www.gravatar.com/avatar/" + sha256(email.toLowerCase())));
      }

      public Author build()
      {
        String firstName = this.firstName;
        if (StringUtil.isEmpty(firstName))
        {
          firstName = null;
        }

        String lastName = this.lastName;
        if (StringUtil.isEmpty(lastName))
        {
          lastName = userID;
        }

        return new Author(userID, firstName, lastName, avatar);
      }

      private static String sha256(String string)
      {
        CheckUtil.checkState(SHA_256, "SHA_256");

        byte[] hash;
        synchronized (SHA_256)
        {
          hash = SHA_256.digest(string.getBytes(StandardCharsets.UTF_8));
        }

        return HexUtil.bytesToHex(hash);
      }

      static
      {
        MessageDigest digest;

        try
        {
          digest = MessageDigest.getInstance("SHA-256");
        }
        catch (Throwable ex)
        {
          OM.LOG.warn(ex);
          digest = null;
        }

        SHA_256 = digest;
      }
    }

    /**
     * @author Eike Stepper
     */
    public static final class Cache
    {
      private static final Map<Object, Cache> CACHES = new WeakHashMap<>();

      private final Map<String, Author> authors = new ConcurrentHashMap<>();

      private final Function<Iterable<String>, Map<String, Author>> authorsLoader;

      public Cache(Function<Iterable<String>, Map<String, Author>> authorsLoader)
      {
        this.authorsLoader = authorsLoader;
      }

      public Author getAuthor(String userID)
      {
        Author author = authors.get(userID);
        if (author == null)
        {
          Map<String, Author> loadedAuthors = authorsLoader.apply(Collections.singleton(userID));

          author = loadedAuthors.get(userID);
          if (author != null)
          {
            authors.put(userID, author);
          }
        }

        return author;
      }

      public Map<String, Author> getAuthors(Iterable<String> userIDs)
      {
        Map<String, Author> result = new HashMap<>();
        List<String> userIDsToLoad = null;

        for (String userID : userIDs)
        {
          Author author = authors.get(userID);
          if (author != null)
          {
            result.put(userID, author);
          }
          else
          {
            if (userIDsToLoad == null)
            {
              userIDsToLoad = new ArrayList<>();
            }

            userIDsToLoad.add(userID);
          }
        }

        if (userIDsToLoad != null)
        {
          Map<String, Author> loadedAuthors = authorsLoader.apply(userIDsToLoad);
          if (loadedAuthors != null)
          {
            authors.putAll(loadedAuthors);
            result.putAll(loadedAuthors);
          }
        }

        return result;
      }

      public static Cache of(Object object, Function<Iterable<String>, Map<String, Author>> authorsLoader)
      {
        return of(object, () -> authorsLoader);
      }

      public static Cache of(Object object, Supplier<Function<Iterable<String>, Map<String, Author>>> authorsLoaderSupplier)
      {
        synchronized (CACHES)
        {
          return CACHES.computeIfAbsent(object, k -> {
            Cache cache = new Cache(authorsLoaderSupplier.get());

            if (object instanceof ILifecycle)
            {
              ((ILifecycle)object).addListener(new LifecycleEventAdapter()
              {
                @Override
                protected void onDeactivated(ILifecycle lifecycle)
                {
                  synchronized (CACHES)
                  {
                    CACHES.remove(lifecycle);
                  }
                }
              });
            }

            return cache;
          });
        }
      }
    }
  }

  /**
   * @author Eike Stepper
   */
  @FunctionalInterface
  public interface Provider
  {
    public ChatMessage[] getMessages();
  }
}
