package org.drone.flipper.repository;

import org.drone.flipper.model.Filters;
import org.drone.flipper.model.Flat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FiltersRepository extends JpaRepository<Filters, Long> {

}
