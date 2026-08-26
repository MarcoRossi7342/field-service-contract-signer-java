package cc.infrai.examples.fieldservice.service;

import cc.infrai.examples.fieldservice.domain.WorkOrder;

public final class SigningDecision {
    public Decision evaluate(WorkOrder order) {
        if (order.dispatchStatus() != WorkOrder.DispatchStatus.COMPLETED) {
            return new Decision(false, "dispatch must be completed");
        }
        if (order.photos().isEmpty()) {
            return new Decision(false, "at least one work photo is required");
        }
        if (order.followUp() == null || order.followUp().summary().isBlank()) {
            return new Decision(false, "technician follow-up is required");
        }
        return new Decision(true, "work evidence is complete");
    }

    public record Decision(boolean approved, String reason) {}
}
