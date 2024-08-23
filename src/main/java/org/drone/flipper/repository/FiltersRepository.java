package org.drone.flipper.repository;

import org.drone.flipper.model.db.Filters;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FiltersRepository extends JpaRepository<Filters, Long> {
}
