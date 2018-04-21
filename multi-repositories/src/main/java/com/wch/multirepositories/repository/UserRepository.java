package com.wch.multirepositories.repository;

import com.wch.multirepositories.entity.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, String> {
}