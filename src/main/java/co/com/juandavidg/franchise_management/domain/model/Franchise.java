package co.com.juandavidg.franchise_management.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder(toBuilder = true)
public class Franchise {
    String id;
    String name;
    Instant createdAt;
    Instant updatedAt;
}
