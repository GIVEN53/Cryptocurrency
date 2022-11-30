package OneCoin.Server.order.service;

import OneCoin.Server.coin.service.CoinService;
import OneCoin.Server.config.auth.utils.LoggedInUserInfoUtils;
import OneCoin.Server.exception.BusinessLogicException;
import OneCoin.Server.helper.StubData;
import OneCoin.Server.order.entity.TransactionHistory;
import OneCoin.Server.order.repository.TransactionHistoryRepository;
import OneCoin.Server.user.repository.UserRepository;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Page;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@MockBean(OkHttpClient.class)
public class TransactionHistoryServiceTest {
    private final int page = 0;
    private final int size = 15;
    @Autowired
    private TransactionHistoryService transactionHistoryService;
    @Autowired
    private UserRepository userRepository;
    @MockBean
    private LoggedInUserInfoUtils loggedInUserInfoUtils;
    @MockBean
    private CoinService coinService;
    @SpyBean
    private TransactionHistoryRepository transactionHistoryRepository;

    @BeforeEach
    void saveTransactionHistory() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        userRepository.save(StubData.MockUser.getMockEntity());
        transactionHistoryRepository.save(StubData.MockHistory.getMockEntity());
    }

    @AfterEach
    void deleteAll() {
        transactionHistoryRepository.deleteAll();
    }

    @ParameterizedTest
    @CsvSource(value = {"w:BID:KRW-BTC:1", "m:ASK::0", "3m:ALL:KRW-BTC:1", "6m:ALL::1"}, delimiter = ':')
    void searchTest(String period, String type, String code, int expectSize) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        // given
        given(loggedInUserInfoUtils.extractUser()).willReturn(StubData.MockUser.getMockEntity());
        given(coinService.findCoin(anyString())).willReturn(StubData.MockCoin.getMockEntity(1L, code, "비트코인"));

        // when
        Page<TransactionHistory> transactionHistoryPage = transactionHistoryService.findTransactionHistory(period, type, code, page, size);
        List<TransactionHistory> transactionHistories = transactionHistoryPage.getContent();

        // then
        assertThat(transactionHistories.size()).isEqualTo(expectSize);
    }

    @Test
    @DisplayName("잘못된 기간을 보내면 에러가 발생한다")
    void periodExceptionTest() {
        // given
        String period = "a";

        // when then
        assertThrows(BusinessLogicException.class, () -> transactionHistoryService.findTransactionHistory(period, "BID", "KRW-BTC", page, size));
    }

    @Test
    @DisplayName("잘못된 타입을 입력하면 에러가 발생한다")
    void typeExceptionTest() {
        // given
        String type = "ABC";

        // when then
        assertThrows(BusinessLogicException.class, () -> transactionHistoryService.findTransactionHistory("w", type, "KRW-BTC", page, size));
    }
}
