package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.ItemV4;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ItemRepositoryV4 extends JpaRepository<ItemV4, String> {

    Optional<ItemV4> findByItemCode(String itemCode);
}