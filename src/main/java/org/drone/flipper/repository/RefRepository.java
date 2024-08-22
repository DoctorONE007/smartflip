package org.drone.flipper.repository;

import org.drone.flipper.model.Ref;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefRepository extends JpaRepository<Ref, Integer> {

}
