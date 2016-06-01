package de.hska.lkit.demo.web;

/**
 * Created by patrickkoenig on 29.05.16.
 */

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class Persistency {

    private RedisAtomicLong userid;

    private RedisAtomicLong postid;

    private StringRedisTemplate stringRedisTemplate;

    private HashOperations<String, String, String> redisStringHashOps;
    private SetOperations<String, String> redisStringSetOps;
    private ListOperations<String, String> redisStringListOps;
    private ValueOperations<String, String> redisStringValueOps;
    private ZSetOperations<String, String> redisStringSortedSetOps;


    @Autowired
    public Persistency(StringRedisTemplate stringRedisTemplate) {
        this.redisStringHashOps = stringRedisTemplate.opsForHash();
        this.redisStringSetOps = stringRedisTemplate.opsForSet();
        this.redisStringSortedSetOps = stringRedisTemplate.opsForZSet();
        this.redisStringListOps = stringRedisTemplate.opsForList();
        this.redisStringValueOps = stringRedisTemplate.opsForValue();
        this.userid = new RedisAtomicLong("userid", stringRedisTemplate.getConnectionFactory());
        this.postid = new RedisAtomicLong("postid", stringRedisTemplate.getConnectionFactory());

    }

    public boolean userExists(User user) {
        if (user == null) {
            throw new NullPointerException("User object cannot be null");
        }

        List<String> users = getAllUsers();
        return users.contains(user.getUsername());
    }

    private List<String> getAllUsers() {
        Set<String> users =redisStringSetOps.members("allusers");
        List<String> usernames = new ArrayList<>();
        for(String userelement : users) {
            String[] elementParts = userelement.split(":");
            usernames.add(elementParts[1]);
        }

        return usernames;
    }

    public boolean createUser(User user) {
        String key = "user:" + user.getUsername();
        redisStringHashOps.put(key, "id", String.valueOf(userid.getAndIncrement()));
        redisStringHashOps.put(key, "username", user.getUsername());
        redisStringHashOps.put(key, "password", user.getPassword());

        redisStringSetOps.add("allusers", key);
        return false;
    }

    public User getUser(User user) {
        User savedUser = new User();
        String key = "user:" + user.getUsername();

        if (userExists(user)) {
            savedUser.setId(Long.parseLong(redisStringHashOps.get(key, "id")));
            savedUser.setUsername(redisStringHashOps.get(key, "username"));
            savedUser.setPassword(redisStringHashOps.get(key, "password"));
            return savedUser;

        } else {
            return null;
        }
    }

    public User getUser (String username){
        User user = new User();
        user.setUsername(username);
        return getUser(user);
    }

    public void createPost(Post post, String username) {

        // generate a unique id
        long realID = postid.incrementAndGet();
        String id = String.valueOf(realID);
        post.setId(id);


        String key = "posts:" + id;
        redisStringHashOps.put(key, "id", id);
        redisStringHashOps.put(key, "content", post.getContent());
        redisStringHashOps.put(key, "date", String.valueOf(post.getDate().getTime()));

        redisStringSortedSetOps.add("allposts:", id, realID);

        String userPostsKey = "user:" + username + ":posts";
        redisStringListOps.rightPush(userPostsKey, id);

        String postToUserKey = "posts:" + id + ":user";
        redisStringValueOps.append(postToUserKey, username);
    }


    public User findUserForPost(String postid) {

        String postToUserKey = "posts:" + postid + ":user";
        String username = redisStringValueOps.get(postToUserKey);
        return getUser(username);
    }

    public List<String> findPostsForUser(String username) {

        return null;
    }

}