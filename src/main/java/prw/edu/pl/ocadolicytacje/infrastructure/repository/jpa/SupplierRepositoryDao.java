package prw.edu.pl.ocadolicytacje.infrastructure.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import prw.edu.pl.ocadolicytacje.infrastructure.entity.SupplierEntity;

@Repository
public interface SupplierRepositoryDao extends JpaRepository<SupplierEntity, Long> {
}
