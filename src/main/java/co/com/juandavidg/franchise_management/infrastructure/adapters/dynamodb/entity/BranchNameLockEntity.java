package co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.util.Locale;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class BranchNameLockEntity {

    private String pk;
    private String sk;
    private String franchiseId;
    private String branchId;

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

    public static String generatePk(final String franchiseId) {
        return "FRANCHISE#" + franchiseId;
    }

    public static String generateSk(final String name) {
        return "BRANCHNAME#" + name.trim().toLowerCase(Locale.ROOT);
    }

    public static BranchNameLockEntity from(final String franchiseId, final String branchId, final String name) {
        return BranchNameLockEntity.builder()
                .pk(generatePk(franchiseId))
                .sk(generateSk(name))
                .franchiseId(franchiseId)
                .branchId(branchId)
                .build();
    }
}
