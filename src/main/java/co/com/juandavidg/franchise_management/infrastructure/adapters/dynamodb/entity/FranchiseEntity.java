package co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class FranchiseEntity {
    
    private String pk;
    private String sk;
    private String id;
    private String name;
    private String nameKey;
    private Instant createdAt;
    private Instant updatedAt;
    
    @DynamoDbPartitionKey
    @DynamoDbAttribute("PK")
    public String getPk() {
        return pk;
    }
    
    @DynamoDbSortKey
    @DynamoDbAttribute("SK")
    public String getSk() {
        return sk;
    }
    
    @DynamoDbAttribute("id")
    public String getId() {
        return id;
    }
    
    @DynamoDbAttribute("name")
    public String getName() {
        return name;
    }

    @DynamoDbAttribute("nameKey")
    public String getNameKey() {
        return nameKey;
    }
    
    @DynamoDbAttribute("createdAt")
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    @DynamoDbAttribute("updatedAt")
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public static String generatePk(final String franchiseId) {
        return "FRANCHISE#" + franchiseId;
    }
    
    public static String generateSk() {
        return "#META";
    }
}
