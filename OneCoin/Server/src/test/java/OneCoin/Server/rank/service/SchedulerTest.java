package OneCoin.Server.rank.service;

import OneCoin.Server.rank.entity.Rank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class SchedulerTest {
    @Autowired
    private RankService rankService;

    @Test
    void test() throws InterruptedException {
        Thread.sleep(2000);
        List<Rank> top10 = rankService.getTop10();

        double firstRoi = Double.valueOf(top10.get(0).getRoi().replace("%",""));
        double secondRoi = Double.valueOf(top10.get(1).getRoi().replace("%",""));
        double diff = firstRoi - secondRoi;
        assertThat(diff)
                .isPositive();
    }
}
