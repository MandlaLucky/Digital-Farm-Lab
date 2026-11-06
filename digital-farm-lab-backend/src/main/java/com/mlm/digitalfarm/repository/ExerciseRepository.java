package com.mlm.digitalfarm.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import com.mlm.digitalfarm.entity.Exercise;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {

    List<Exercise> findByModuleCode(String moduleCode);

    List<Exercise> findAllByOrderByModuleCodeAscTitleAsc();
}