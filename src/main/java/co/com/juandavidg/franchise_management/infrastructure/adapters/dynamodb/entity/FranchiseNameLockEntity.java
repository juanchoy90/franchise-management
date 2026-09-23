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
public class FranchiseNameLockEntity {

    private String pk;
    private String sk;
    private String franchiseId;

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

    public static String generatePk(final String name) {
        return "FRANCHISE#NAME#" + name.trim().toLowerCase(Locale.ROOT);
    }

    public static String generateSk() {
        return "UNIQUE";
    }

    public static FranchiseNameLockEntity from(final String franchiseId, final String name) {
        return FranchiseNameLockEntity.builder()
                .pk(generatePk(name))
                .sk(generateSk())
                .franchiseId(franchiseId)
                .build();
    }
}
