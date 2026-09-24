package co.com.juandavidg.franchise_management.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder(toBuilder = true)
public class Branch {
    String id;
    String franchiseId;
    String name;
    Instant createdAt;
    Instant updatedAt;
}
