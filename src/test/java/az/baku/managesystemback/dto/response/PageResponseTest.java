package az.baku.managesystemback.dto.response;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

    @Test
    void fromShouldMapPageCorrectly() {
        List<String> content = List.of("one", "two");

        Page<String> page = new PageImpl<>(
                content,
                PageRequest.of(1, 2),
                5
        );

        PageResponse<String> response = PageResponse.from(page);

        assertThat(response.data()).containsExactly("one", "two");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
    }
}
