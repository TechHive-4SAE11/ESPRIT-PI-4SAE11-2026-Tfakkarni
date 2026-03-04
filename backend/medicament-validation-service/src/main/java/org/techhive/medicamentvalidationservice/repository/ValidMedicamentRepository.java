package org.techhive.medicamentvalidationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.techhive.medicamentvalidationservice.entity.ValidMedicament;

import java.util.List;

@Repository
public interface ValidMedicamentRepository extends JpaRepository<ValidMedicament, Long> {

    boolean existsByDrugNameIgnoreCase(String drugName);

    boolean existsByBrandNameIgnoreCase(String brandName);

    boolean existsByGenericNameIgnoreCase(String genericName);

    @Query("SELECT DISTINCT v.drugName FROM ValidMedicament v WHERE LOWER(v.drugName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<String> findSimilarDrugNames(@Param("name") String name);

    @Query("SELECT DISTINCT v.brandName FROM ValidMedicament v WHERE LOWER(v.brandName) LIKE LOWER(CONCAT('%', :name, '%')) AND v.brandName IS NOT NULL")
    List<String> findSimilarBrandNames(@Param("name") String name);

    @Query("SELECT DISTINCT v.genericName FROM ValidMedicament v WHERE LOWER(v.genericName) LIKE LOWER(CONCAT('%', :name, '%')) AND v.genericName IS NOT NULL")
    List<String> findSimilarGenericNames(@Param("name") String name);

    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM ValidMedicament v " +
           "WHERE LOWER(v.drugName) = LOWER(:name) " +
           "OR LOWER(v.brandName) = LOWER(:name) " +
           "OR LOWER(v.genericName) = LOWER(:name) " +
           "OR LOWER(v.activeIngredients) LIKE LOWER(CONCAT('%', :name, '%'))")
    boolean existsByAnyNameIgnoreCase(@Param("name") String name);

    long count();
}
