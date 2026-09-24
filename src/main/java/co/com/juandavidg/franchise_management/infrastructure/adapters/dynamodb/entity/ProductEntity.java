package co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class ProductEntity {
    
    private String pk;
    private String sk;
    private String id;
    private String franchiseId;
    private String branchId;
    private String branchName;
    private String name;
    private String nameKey;
    private String gsi1Pk;
    private Integer stock;
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
    
    @DynamoDbAttribute("franchiseId")
    public String getFranchiseId() {
        return franchiseId;
    }

    @DynamoDbAttribute("branchId")
    public String getBranchId() {
        return branchId;
    }
    
    @DynamoDbAttribute("branchName")
    public String getBranchName() {
        return branchName;
    }
    
    @DynamoDbAttribute("name")
    public String getName() {
        return name;
    }

    @DynamoDbAttribute("nameKey")
    public String getNameKey() {
        return nameKey;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "GSI1")
    @DynamoDbAttribute("GSI1PK")
    public String getGsi1Pk() {
        return gsi1Pk;
    }

    @DynamoDbSecondarySortKey(indexNames = "GSI1")
    @DynamoDbAttribute("stock")
    public Integer getStock() {
        return stock;
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
    
    public static String generateSk(final String branchId, final String productId) {
        return "PRODUCT#" + branchId + "#" + productId;
    }

    public static String generateGsi1Pk(final String franchiseId, final String branchId) {
        return "BRANCH#" + franchiseId + "#" + branchId;
    }
}
