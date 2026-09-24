package co.com.juandavidg.franchise_management.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder(toBuilder = true)
public class Product {
    String id;
    String franchiseId;
    String branchId;
    String name;
    Integer stock;
    Instant createdAt;
    Instant updatedAt;
}
