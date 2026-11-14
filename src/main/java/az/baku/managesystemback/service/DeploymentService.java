package az.baku.managesystemback.service;

import az.baku.managesystemback.dto.request.CreateDeploymentRequest;
import az.baku.managesystemback.dto.response.DeploymentResponse;
import az.baku.managesystemback.dto.response.PublicSiteResponse;

import java.util.List;

/**
 * @author Fatima Sultanova
 */
public interface DeploymentService {

    List<DeploymentResponse> listDeployments(long userId, Long siteId, String status);

    DeploymentResponse getDeployment(long userId, long deploymentId);

    DeploymentResponse createDeployment(long userId, CreateDeploymentRequest request);

    DeploymentResponse redeploy(long userId, long deploymentId);

    DeploymentResponse stop(long userId, long deploymentId);

    DeploymentResponse rollback(long userId, long deploymentId);

    PublicSiteResponse getPublicLiveSite(String slug);
}
