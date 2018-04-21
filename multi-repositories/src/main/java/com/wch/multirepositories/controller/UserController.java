package com.wch.multirepositories.controller;

import com.wch.multirepositories.entity.User;
import com.wch.multirepositories.readrepository.UserReadRepository;
import com.wch.multirepositories.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserReadRepository readRepository;

    @Autowired
    @Qualifier("seconduserRepository")
    private UserRepository seconduserRepository;

    @PostConstruct
    public void init() {
        User user = new User();
        user.setId("write");
        user.setName("I comes from write db!");
        userRepository.save(user);
        user.setId("read");
        user.setName("I comes from read db!");
        readRepository.save(user);
    }

    @GetMapping("/findAll")
    public List<User> findAll() {
        User user = new User();
        user.setId("-----");
        user.setName("----------");
        List<User> users = new ArrayList<>();
        users.addAll((Collection<? extends User>) userRepository.findAll());
        users.add(user);
        users.addAll((Collection<? extends User>) readRepository.findAll());
        users.add(user);
        users.addAll((Collection<? extends User>) seconduserRepository.findAll());
        return users;
    }

}
