package co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.entity;

import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.helper.NameNormalizer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class ProductNameLockEntity {

    private String pk;
    private String sk;
    private String franchiseId;
    private String branchId;
    private String productId;

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

    @DynamoDbAttribute("franchiseId")
    public String getFranchiseId() {
        return franchiseId;
    }

    @DynamoDbAttribute("branchId")
    public String getBranchId() {
        return branchId;
    }

    @DynamoDbAttribute("productId")
    public String getProductId() {
        return productId;
    }

    public static String generatePk(final String franchiseId) {
        return "FRANCHISE#" + franchiseId;
    }

    public static String generateSk(final String branchId, final String name) {
        return "UNIQ#PRODUCT#" + branchId + "#" + NameNormalizer.normalize(name);
    }

    public static ProductNameLockEntity from(
            final String franchiseId,
            final String branchId,
            final String productId,
            final String name) {
        return ProductNameLockEntity.builder()
                .pk(generatePk(franchiseId))
                .sk(generateSk(branchId, name))
                .franchiseId(franchiseId)
                .branchId(branchId)
                .productId(productId)
                .build();
    }
}
