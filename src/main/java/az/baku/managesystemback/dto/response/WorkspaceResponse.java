package az.baku.managesystemback.dto.response;

import az.baku.managesystemback.entity.Workspace;

public record WorkspaceResponse(String id, String name, String ownerId, String createdAt) {
    public static WorkspaceResponse from(Workspace w) {
        return new WorkspaceResponse(Long.toString(w.getId()), w.getName(), Long.toString(w.getOwner().getId()), w.getCreatedAt().toString());
    }
}
