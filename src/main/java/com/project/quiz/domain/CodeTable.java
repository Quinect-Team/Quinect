package com.project.quiz.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "code_table")
@Getter
@Setter
public class CodeTable {
	@Id
	private String code;
	private String groupId;
	private String name;
	private String description;

}