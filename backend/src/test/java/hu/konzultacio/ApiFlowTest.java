package hu.konzultacio;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ApiFlowTest extends IntegrationTestBase {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    private String register(String role) throws Exception {
        String body = om.writeValueAsString(Map.of("email", UUID.randomUUID() + "@test.hu",
                "password", "Password123!", "fullName", "Teszt Elek", "role", role));
        String res = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return "Bearer " + om.readTree(res).get("token").asText();
    }

    private String slotJson() throws Exception {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        return om.writeValueAsString(Map.of("startsAt", start.toString(), "endsAt", start.plus(30, ChronoUnit.MINUTES).toString()));
    }

    @Test
    void protectedEndpointWithoutTokenIs401() throws Exception {
        mvc.perform(get("/api/slots")).andExpect(status().isUnauthorized());
    }

    @Test
    void studentCannotCreateSlot() throws Exception {
        mvc.perform(post("/api/slots").header("Authorization", register("STUDENT"))
                .contentType(MediaType.APPLICATION_JSON).content(slotJson())).andExpect(status().isForbidden());
    }

    @Test
    void teacherCreatesSlotStudentBooksOnceOnly() throws Exception {
        String teacher = register("TEACHER");
        String student = register("STUDENT");
        String res = mvc.perform(post("/api/slots").header("Authorization", teacher)
                .contentType(MediaType.APPLICATION_JSON).content(slotJson()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = om.readTree(res).get("id").asLong();

        mvc.perform(post("/api/slots/" + id + "/bookings").header("Authorization", student)).andExpect(status().isCreated());
        mvc.perform(post("/api/slots/" + id + "/bookings").header("Authorization", student)).andExpect(status().isConflict());
        mvc.perform(post("/api/slots/" + id + "/bookings").header("Authorization", teacher)).andExpect(status().isForbidden());
    }
}
