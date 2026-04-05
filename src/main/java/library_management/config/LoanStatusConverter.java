package library_management.config;

import library_management.models.LoanStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

/**
 * Custom MongoDB converter để map old string status sang new LoanStatus enum
 * 
 * Mapping:
 * "ACTIVE" → PICKED_UP (loan đã được nhân viên xác nhận lấy)
 * "RETURNED" → RETURNED
 * "CANCELLED" → CANCELLED
 * "PENDING" → PENDING
 */
@ReadingConverter
public class LoanStatusConverter implements Converter<String, LoanStatus> {

    @Override
    public LoanStatus convert(String source) {
        if (source == null) {
            return LoanStatus.PENDING; // Default
        }

        return switch (source.toUpperCase()) {
            case "ACTIVE" -> LoanStatus.PICKED_UP;      // Old data: ACTIVE → New: PICKED_UP
            case "PICKED_UP" -> LoanStatus.PICKED_UP;
            case "PENDING" -> LoanStatus.PENDING;
            case "RETURNED" -> LoanStatus.RETURNED;
            case "CANCELLED" -> LoanStatus.CANCELLED;
            default -> {
                System.err.println("⚠️ Unknown LoanStatus: " + source + ", default to PENDING");
                yield LoanStatus.PENDING;
            }
        };
    }
}
