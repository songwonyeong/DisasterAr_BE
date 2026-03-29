package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.ItemV4;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepositoryV4 extends JpaRepository<ItemV4, String> {
}