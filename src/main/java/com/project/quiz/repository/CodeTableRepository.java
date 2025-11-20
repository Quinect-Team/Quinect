package com.project.quiz.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.quiz.domain.CodeTable;

public interface CodeTableRepository extends JpaRepository<CodeTable, String> {
	List<CodeTable> findByGroupId(String groupId);
}
