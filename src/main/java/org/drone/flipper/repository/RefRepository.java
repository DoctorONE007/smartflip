package org.drone.flipper.repository;

import org.drone.flipper.model.db.Referral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefRepository extends JpaRepository<Referral, Integer> {
}
