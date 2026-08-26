package cc.infrai.examples.fieldservice.service;

import cc.infrai.examples.fieldservice.domain.WorkOrder;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SigningDecisionTest {
    private final SigningDecision decision = new SigningDecision();

    @Test
    void approvesOnlyCompletedWorkWithPhotoEvidenceAndFollowUp() {
        WorkOrder complete = order(WorkOrder.DispatchStatus.COMPLETED, List.of(photo()), followUp());
        WorkOrder stillOnSite = order(WorkOrder.DispatchStatus.ON_SITE, List.of(photo()), followUp());
        WorkOrder missingPhotos = order(WorkOrder.DispatchStatus.COMPLETED, List.of(), followUp());

        assertTrue(decision.evaluate(complete).approved());
        assertFalse(decision.evaluate(stillOnSite).approved());
        assertEquals("at least one work photo is required", decision.evaluate(missingPhotos).reason());
    }

    private WorkOrder order(WorkOrder.DispatchStatus status, List<WorkOrder.WorkPhoto> photos,
                            WorkOrder.TechnicianFollowUp followUp) {
        return new WorkOrder("WO-TEST", "Learning Center", "1 Course Way", status, photos, followUp);
    }

    private WorkOrder.WorkPhoto photo() {
        return new WorkOrder.WorkPhoto("completed repair", "photos/completed.jpg", Instant.EPOCH);
    }

    private WorkOrder.TechnicianFollowUp followUp() {
        return new WorkOrder.TechnicianFollowUp("Technician", "Verified classroom controls", Instant.EPOCH);
    }
}
