package com.celonis.challenge.persistence.repository;

import com.celonis.challenge.persistence.entities.FileTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileTaskRepository extends JpaRepository<FileTaskEntity, String> {
}