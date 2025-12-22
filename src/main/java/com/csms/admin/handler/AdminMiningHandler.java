package com.csms.admin.handler;

import com.csms.admin.dto.*;
import com.csms.admin.service.*;
import com.csms.common.handler.BaseHandler;
import com.csms.common.utils.ErrorHandler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class AdminMiningHandler extends BaseHandler {
    
    private final AdminMiningService miningService;
    private final AdminMiningExportService miningExportService;
    private final AdminMiningConditionService conditionService;
    private final AdminMiningBoosterService boosterService;
    private final AdminMiningHistoryListService historyListService;
    private final AdminMiningHistoryListExportService historyListExportService;
    private final AdminReferralBonusService referralBonusService;
    private final AdminRankingRewardService rankingRewardService;
    
    public AdminMiningHandler(
        Vertx vertx,
        AdminMiningService miningService,
        AdminMiningExportService miningExportService,
        AdminMiningConditionService conditionService,
        AdminMiningBoosterService boosterService,
        AdminMiningHistoryListService historyListService,
        AdminMiningHistoryListExportService historyListExportService,
        AdminReferralBonusService referralBonusService,
        AdminRankingRewardService rankingRewardService
    ) {
        super(vertx);
        this.miningService = miningService;
        this.miningExportService = miningExportService;
        this.conditionService = conditionService;
        this.boosterService = boosterService;
        this.historyListService = historyListService;
        this.historyListExportService = historyListExportService;
        this.referralBonusService = referralBonusService;
        this.rankingRewardService = rankingRewardService;
    }
    
    public Router getRouter() {
        Router router = Router.router(vertx);
        
        // Mining Records
        router.get("/records").handler(this::getMiningRecords);
        router.get("/records/export").handler(this::exportMiningRecords);
        
        // Mining Conditions
        router.get("/conditions").handler(this::getMiningConditions);
        router.patch("/conditions/basic").handler(this::updateBasicConditions);
        router.patch("/conditions/progress").handler(this::updateProgressSetting);
        router.patch("/conditions/level-limit").handler(this::updateLevelLimit);
        router.patch("/conditions/level-limits-enabled").handler(this::updateLevelLimitsEnabled);
        
        // Mining Booster
        router.get("/booster").handler(this::getMiningBoosters);
        router.patch("/booster").handler(this::updateBooster);
        
        // Mining History List
        router.get("/history").handler(this::getMiningHistoryList);
        router.get("/history/export").handler(this::exportMiningHistoryList);
        
        // Referral Bonus
        router.get("/referral-bonus").handler(this::getReferralBonus);
        router.patch("/referral-bonus").handler(this::updateReferralBonus);
        
        // Ranking Reward
        router.get("/ranking-reward").handler(this::getRankingReward);
        router.patch("/ranking-reward").handler(this::updateRankingReward);
        
        return router;
    }
    
    private void getMiningRecords(RoutingContext ctx) {
        try {
            Integer limit = getQueryParamAsInteger(ctx, "limit", 20);
            Integer offset = getQueryParamAsInteger(ctx, "offset", 0);
            String dateRange = ctx.queryParams().get("dateRange");
            String startDate = ctx.queryParams().get("startDate");
            String endDate = ctx.queryParams().get("endDate");
            String searchCategory = ctx.queryParams().get("searchCategory");
            String searchKeyword = ctx.queryParams().get("searchKeyword");
            String activityStatus = ctx.queryParams().get("activityStatus");
            
            miningService.getMiningRecords(
                limit,
                offset,
                dateRange,
                startDate,
                endDate,
                searchCategory,
                searchKeyword,
                activityStatus
            ).onSuccess(result -> {
                success(ctx, result);
            }).onFailure(throwable -> {
                ctx.fail(throwable);
                ErrorHandler.handle(ctx);
            });
        } catch (Exception e) {
            ctx.fail(e);
            ErrorHandler.handle(ctx);
        }
    }
    
    private void exportMiningRecords(RoutingContext ctx) {
        try {
            String dateRange = ctx.queryParams().get("dateRange");
            String startDate = ctx.queryParams().get("startDate");
            String endDate = ctx.queryParams().get("endDate");
            String searchCategory = ctx.queryParams().get("searchCategory");
            String searchKeyword = ctx.queryParams().get("searchKeyword");
            String activityStatus = ctx.queryParams().get("activityStatus");
            
            miningExportService.exportToExcel(
                dateRange,
                startDate,
                endDate,
                searchCategory,
                searchKeyword,
                activityStatus
            ).onSuccess(buffer -> {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String filename = "mining_records_" + timestamp + ".xlsx";
                
                ctx.response()
                    .putHeader("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .putHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .putHeader("Content-Length", String.valueOf(buffer.length()))
                    .end(buffer);
            }).onFailure(throwable -> {
                ctx.fail(throwable);
                ErrorHandler.handle(ctx);
            });
        } catch (Exception e) {
            ctx.fail(e);
            ErrorHandler.handle(ctx);
        }
    }
    
    // Mining Conditions
    private void getMiningConditions(RoutingContext ctx) {
        try {
            conditionService.getMiningConditions()
                .onSuccess(result -> {
                    success(ctx, result);
                })
                .onFailure(throwable -> {
                    ctx.fail(throwable);
                    ErrorHandler.handle(ctx);
                });
        } catch (Exception e) {
            ctx.fail(e);
            ErrorHandler.handle(ctx);
        }
    }
    
    private void updateBasicConditions(RoutingContext ctx) {
        try {
            JsonObject body = ctx.body().asJsonObject();
            UpdateBasicConditionRequestDto request = body.mapTo(UpdateBasicConditionRequestDto.class);
            
            conditionService.updateBasicConditions(request)
                .onSuccess(result -> {
                    success(ctx, new JsonObject().put("message", "Basic conditions updated"));
                })
                .onFailure(throwable -> {
                    ctx.fail(throwable);
                    ErrorHandler.handle(ctx);
                });
        } catch (Exception e) {
            ctx.fail(e);
            ErrorHandler.handle(ctx);
        }
    }
    
    private void updateProgressSetting(RoutingContext ctx) {
        try {
            JsonObject body = ctx.body().asJsonObject();
            UpdateProgressSettingRequestDto request = body.mapTo(UpdateProgressSettingRequestDto.class);
            
            conditionService.updateProgressSetting(request)
                .onSuccess(result -> {
                    success(ctx, new JsonObject().put("message", "Progress setting updated"));
                })
                .onFailure(throwable -> {
                    ctx.fail(throwable);
                    ErrorHandler.handle(ctx);
                });
        } catch (Exception e) {
            ctx.fail(e);
            ErrorHandler.handle(ctx);
        }
    }
    
    private void updateLevelLimit(RoutingContext ctx) {
        try {
            JsonObject body = ctx.body().asJsonObject();
            UpdateLevelLimitRequestDto request = body.mapTo(UpdateLevelLimitRequestDto.class);
            
            conditionService.updateLevelLimit(request)
                .onSuccess(result -> {
                    success(ctx, new JsonObject().put("message", "Level limit updated"));
                })
                .onFailure(throwable -> {
                    ctx.fail(throwable);
                    ErrorHandler.handle(ctx);
                });
        } catch (Exception e) {
            ctx.fail(e);
            ErrorHandler.handle(ctx);
        }
    }
    
    private void updateLevelLimitsEnabled(RoutingContext ctx) {
        try {
            JsonObject body = ctx.body().asJsonObject();
            UpdateLevelLimitsEnabledRequestDto request = body.mapTo(UpdateLevelLimitsEnabledRequestDto.class);
            
            conditionService.updateLevelLimitsEnabled(request)
                .onSuccess(result -> {
                    success(ctx, new JsonObject().put("message", "Level limits enabled updated"));
                })
                .onFailure(throwable -> {
                    ctx.fail(throwable);
                    ErrorHandler.handle(ctx);
                });
        } catch (Exception e) {
            ctx.fail(e);
            ErrorHandler.handle(ctx);
        }
    }
    
    // Mining Booster
    private void getMiningBoosters(RoutingContext ctx) {
        try {
            boosterService.getMiningBoosters()
                .onSuccess(result -> {
                    success(ctx, result);
                })
                .onFailure(throwable -> {
                    ctx.fail(throwable);
                    ErrorHandler.handle(ctx);
                });
        } catch (Exception e) {
            ctx.fail(e);
            ErrorHandler.handle(ctx);
        }
    }
    
    private void updateBooster(RoutingContext ctx) {
        try {
            JsonObject body = ctx.body().asJsonObject();
            UpdateBoosterRequestDto request = body.mapTo(UpdateBoosterRequestDto.class);
            
            boosterService.updateBooster(request)
                .onSuccess(result -> {
                    success(ctx, new JsonObject().put("message", "Booster updated"));
                })
                .onFailure(throwable -> {
                    ctx.fail(throwable);
                    ErrorHandler.handle(ctx);
                });
        } catch (Exception e) {
            ctx.fail(e);
            ErrorHandler.handle(ctx);
        }
    }
    
    // Mining History List
    private void getMiningHistoryList(RoutingContext ctx) {
        try {
            Integer limit = getQueryParamAsInteger(ctx, "limit", 20);
            Integer offset = getQueryParamAsInteger(ctx, "offset", 0);
            String searchCategory = ctx.queryParams().get("searchCategory");
            String searchKeyword = ctx.queryParams().get("searchKeyword");
            String sortType = ctx.queryParams().get("sortType");
            String activityStatus = ctx.queryParams().get("activityStatus");
            String sanctionStatus = ctx.queryParams().get("sanctionStatus");
            
            historyListService.getMiningHistoryList(limit, offset, searchCategory, searchKeyword, sortType, activityStatus, sanctionStatus)
                .onSuccess(result -> {
                    success(ctx, result);
                })
                .onFailure(throwable -> {
                    ctx.fail(throwable);
                    ErrorHandler.handle(ctx);
                });
        } catch (Exception e) {
            ctx.fail(e);
            ErrorHandler.handle(ctx);
        }
    }
    
    private void exportMiningHistoryList(RoutingContext ctx) {
        try {
            String searchCategory = ctx.queryParams().get("searchCategory");
            String searchKeyword = ctx.queryParams().get("searchKeyword");
            String sortType = ctx.queryParams().get("sortType");
            String activityStatus = ctx.queryParams().get("activityStatus");
            String sanctionStatus = ctx.queryParams().get("sanctionStatus");
            
            historyListExportService.exportToExcel(searchCategory, searchKeyword, sortType, activityStatus, sanctionStatus)
                .onSuccess(buffer -> {
                    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                    String filename = "mining_history_list_" + timestamp + ".xlsx";
                    
                    ctx.response()
                        .putHeader("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        .putHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                        .putHeader("Content-Length", String.valueOf(buffer.length()))
                        .end(buffer);
                })
                .onFailure(throwable -> {
                    ctx.fail(throwable);
                    ErrorHandler.handle(ctx);
                });
        } catch (Exception e) {
            ctx.fail(e);
            ErrorHandler.handle(ctx);
        }
    }
    
    // Referral Bonus
    private void getReferralBonus(RoutingContext ctx) {
        try {
            referralBonusService.getReferralBonus()
                .onSuccess(result -> {
                    success(ctx, result);
                })
                .onFailure(throwable -> {
                    ctx.fail(throwable);
                    ErrorHandler.handle(ctx);
                });
        } catch (Exception e) {
            ctx.fail(e);
            ErrorHandler.handle(ctx);
        }
    }
    
    private void updateReferralBonus(RoutingContext ctx) {
        try {
            JsonObject body = ctx.body().asJsonObject();
            UpdateReferralBonusRequestDto request = body.mapTo(UpdateReferralBonusRequestDto.class);
            
            referralBonusService.updateReferralBonus(request)
                .onSuccess(result -> {
                    success(ctx, new JsonObject().put("message", "Referral bonus updated"));
                })
                .onFailure(throwable -> {
                    ctx.fail(throwable);
                    ErrorHandler.handle(ctx);
                });
        } catch (Exception e) {
            ctx.fail(e);
            ErrorHandler.handle(ctx);
        }
    }
    
    // Ranking Reward
    private void getRankingReward(RoutingContext ctx) {
        try {
            rankingRewardService.getRankingReward()
                .onSuccess(result -> {
                    success(ctx, result);
                })
                .onFailure(throwable -> {
                    ctx.fail(throwable);
                    ErrorHandler.handle(ctx);
                });
        } catch (Exception e) {
            ctx.fail(e);
            ErrorHandler.handle(ctx);
        }
    }
    
    private void updateRankingReward(RoutingContext ctx) {
        try {
            JsonObject body = ctx.body().asJsonObject();
            UpdateRankingRewardRequestDto request = body.mapTo(UpdateRankingRewardRequestDto.class);
            
            rankingRewardService.updateRankingReward(request)
                .onSuccess(result -> {
                    success(ctx, new JsonObject().put("message", "Ranking reward updated"));
                })
                .onFailure(throwable -> {
                    ctx.fail(throwable);
                    ErrorHandler.handle(ctx);
                });
        } catch (Exception e) {
            ctx.fail(e);
            ErrorHandler.handle(ctx);
        }
    }
}

