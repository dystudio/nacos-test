package com.example.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.first.domain.*;
import com.example.first.mapper.*;
import com.example.second.domain.Interaction;
import com.example.second.mapper.InteractionMapper;
import com.example.second.mapper.SecondMapper;
import com.example.service.DsService;
import com.example.third.domain.*;
import com.example.third.mapper.*;
import com.example.utils.SnowFlake;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * @author wotrd
 */
@Slf4j
@Service
public class DsServiceImpl implements DsService {

    @Autowired
    private FirstMapper firstMapper;

    @Autowired
    private SecondMapper secondMapper;
    //发布记录
    @Autowired
    private ShLearnGardenReportAlbumMapper reportAlbumMapper;
    //收藏点赞（同一条记录点赞和收藏是两条记录）
    @Autowired
    private ShLearnGardenReportAccessMapper reportAccessMapper;
    //记录评论
    @Autowired
    private ShLearnGardenReportCommentMapper reportCommentMapper;
    //发布范围
    @Autowired
    private ShLearnGardenReportDeptMapper reportDeptMapper;
    //附件图片
    @Autowired
    private ShLearnGardenReportPhotoMapper reportPhotoMapper;
    //评论组件
    @Autowired
    private InteractionMapper interactionMapper;
    //发布记录文件表
    @Autowired
    private NoteRecodeFileMapper recodeFileMapper;
    //发布记录表
    @Autowired
    private NoteRecordMapper recordMapper;
    //发布记录可见范围表
    @Autowired
    private NoteRecordScopeMapper recordScopeMapper;

    private static final String SUBJECT_NO = "287298822151999488";

    @Override
    public void transfer() {
        log.info("迁移老数据开始");
        //查询班级项目全部记录
        List<ShLearnGardenReportAlbum> list = reportAlbumMapper.selectAll();
        //遍历全部列表封装对象
        list.forEach(reportAlbum -> {
            //
            reportAlbum.setUserOrgId("330108-S000067");
            if (null != reportAlbum.getUserOrgId()) {

                NoteRecord noteRecord = recordMapper.selectById(reportAlbum.getPkId() + "");
                //笔记记录对象
                if (null != noteRecord) {
                    recordMapper.deleteByPrimaryKey(reportAlbum.getPkId() + "");
                    recodeFileMapper.deleteByRecordId(reportAlbum.getPkId() + "");
                    recordScopeMapper.deleteByRecordId(reportAlbum.getPkId() + "");
                }
                handlerRecord(reportAlbum);
                handlerPhoto(reportAlbum);
                handlerScope(reportAlbum);

                //评论列表
                List<ShLearnGardenReportComment> commentList = reportCommentMapper.selectById(reportAlbum.getPkId());
                commentList.forEach(reportComment -> {
                    CommentExtendBO extendBO = new CommentExtendBO(reportComment.getCommentContent(), reportComment.getCommentUserId(),
                            reportComment.getCommentUserName(), false);
                    Interaction interaction = Interaction.builder()
                            .subjectNo(SUBJECT_NO)
                            .extend(JSONObject.parseObject(JSONObject.toJSONString(extendBO)))
                            .fromUserId(reportComment.getCommentUserId())
                            .fromUserName(null == reportComment.getCommentUserName() ? "" : reportComment.getCommentUserName())
                            .gmtCreate(reportAlbum.getCreateDate())
                            .gmtModify(reportAlbum.getCreateDate())
                            .interactionNo(SnowFlake.nextSIdStr())
                            .isDelete(false)
                            .momentId(reportAlbum.getPkId() + "")
                            .orgId(null == reportAlbum.getUserOrgId() ? "" : reportAlbum.getUserOrgId())
                            .read(false)
                            .type("COMMENT")
                            .build();
                    interactionMapper.insert(interaction);
                });
                //点赞收藏列表
                List<ShLearnGardenReportAccess> reportAccessList = reportAccessMapper.selectById(reportAlbum.getPkId());

                reportAccessList.forEach(reportAccess -> {
                    if (reportAccess.getIsHouse().equals("0")) {
                        LikeExtendBO extendBO = new LikeExtendBO(reportAccess.getAccessUserId());
                        Interaction interaction = Interaction.builder()
                                .subjectNo(SUBJECT_NO)
                                .extend(JSONObject.parseObject(JSONObject.toJSONString(extendBO)))
                                .fromUserId(reportAccess.getAccessUserId())
                                .fromUserName(null == reportAccess.getAccessUserName() ? "" : reportAccess.getAccessUserName())
                                .gmtCreate(reportAlbum.getCreateDate())
                                .gmtModify(reportAlbum.getCreateDate())
                                .interactionNo(SnowFlake.nextSIdStr())
                                .isDelete(false)
                                .momentId(reportAccess.getPkId() + "")
                                .orgId(null == reportAlbum.getUserOrgId() ? "" : reportAlbum.getUserOrgId())
                                .read(false)
                                .type("COLLECT")
                                .build();
                        interactionMapper.insert(interaction);
                    }
                    if (reportAccess.getStatus().equals("1")) {
                        LikeExtendBO extendBO = new LikeExtendBO(reportAccess.getAccessUserId());
                        Interaction interaction = Interaction.builder()
                                .subjectNo(SUBJECT_NO)
                                .extend(JSONObject.parseObject(JSONObject.toJSONString(extendBO)))
                                .fromUserId(reportAccess.getAccessUserId())
                                .fromUserName(null == reportAccess.getAccessUserName() ? "" : reportAccess.getAccessUserName())
                                .gmtCreate(reportAlbum.getCreateDate())
                                .gmtModify(reportAlbum.getCreateDate())
                                .interactionNo(SnowFlake.nextSIdStr())
                                .isDelete(false)
                                .momentId(reportAccess.getPkId() + "")
                                .orgId(null == reportAlbum.getUserOrgId() ? "" : reportAlbum.getUserOrgId())
                                .read(false)
                                .type("LIKE")
                                .build();
                        interactionMapper.insert(interaction);
                    }
                });

            }

        });
        log.info("迁移老数据完成");

    }

    /**
     * 处理范围
     *
     * @param reportAlbum
     */
    private void handlerScope(ShLearnGardenReportAlbum reportAlbum) {
        //发布范围
        List<ShLearnGardenReportDept> reportDepts = reportDeptMapper.selectByPkId(reportAlbum.getPkId());
        reportDepts.forEach(reportDept -> {
            NoteRecordScope recordScope = NoteRecordScope.builder()
                    .createUser(reportAlbum.getCreateWxUserId())
                    .deleted(false)
                    .deptId(null == reportDept.getDeptId() ? "" : reportDept.getDeptId())
                    .deptName(null == reportDept.getDeptName() ? "" : reportDept.getDeptName())
                    .gmtCreate(reportAlbum.getCreateDate())
                    .gmtModify(reportAlbum.getCreateDate())
                    .id(getId())
                    .modifyUser(reportAlbum.getCreateWxUserId())
                    .orgId(null == reportAlbum.getUserOrgId() ? "" : reportAlbum.getUserOrgId())
                    .recordId(reportAlbum.getPkId() + "")
                    .version(1)
                    .build();
            recordScopeMapper.insert(recordScope);
        });

    }

    /**
     * 处理图片
     *
     * @param reportAlbum
     */
    private void handlerPhoto(ShLearnGardenReportAlbum reportAlbum) {
        //查询附件文件
        List<ShLearnGardenReportPhoto> reportPhotos = reportPhotoMapper.selectByPkId(reportAlbum.getPkId());
        reportPhotos.forEach(reportPhoto -> {
            //插入附件文件
            NoteRecordFile recordFile = NoteRecordFile.builder()
                    .content("")
                    .createUser(null == reportAlbum.getCreateUserCard() ? "" : reportAlbum.getCreateUserCard())
                    .deleted(false)
                    .gmtCreate(reportAlbum.getCreateDate())
                    .gmtModify(reportAlbum.getCreateDate())
                    .id(getId())
                    .modifyUser(null == reportAlbum.getCreateUserCard() ? "" : reportAlbum.getCreateUserCard())
                    .orgId(null == reportAlbum.getUserOrgId() ? "" : reportAlbum.getUserOrgId())
                    .recordId(reportPhoto.getAlbumPk() + "")
                    .type(1)
                    .version(1)
                    .build();
            recodeFileMapper.insert(recordFile);
        });

    }

    /**
     * 处理记录
     *
     * @param reportAlbum
     */
    private void handlerRecord(ShLearnGardenReportAlbum reportAlbum) {
        //插入记录
        try {
            NoteRecord noteRecord = NoteRecord.builder()
                    .content(reportAlbum.getAlbumDesc())
                    .createUser(null == reportAlbum.getCreateUserCard() ? "" : reportAlbum.getCreateUserCard())
                    .deleted(false)
                    .deptId(reportAlbum.getXyhDeptIdArr())
                    .deptName(null == reportAlbum.getQueryDeptNameArr() ? "" : reportAlbum.getQueryDeptNameArr())
                    .gmtCreate(reportAlbum.getCreateDate())
                    .gmtModify(reportAlbum.getCreateDate())
                    .modifyUser(null == reportAlbum.getCreateUserCard() ? "" : reportAlbum.getCreateUserCard())
                    .orgId(null == reportAlbum.getUserOrgId() ? "" : reportAlbum.getUserOrgId())
                    .scopeType(0)
                    .userImage(reportAlbum.getCreateHeadImgUrl())
                    .userName(reportAlbum.getCreateWxUserName())
                    .version(1)
                    .id(reportAlbum.getPkId() + "")
                    .build();
            recordMapper.insert(noteRecord);
        } catch (Exception e) {
            NoteRecord noteRecord = NoteRecord.builder()
                    .content("")
                    .createUser(null == reportAlbum.getCreateUserCard() ? "" : reportAlbum.getCreateUserCard())
                    .deleted(false)
                    .deptId(reportAlbum.getXyhDeptIdArr())
                    .deptName(null == reportAlbum.getQueryDeptNameArr() ? "" : reportAlbum.getQueryDeptNameArr())
                    .gmtCreate(reportAlbum.getCreateDate())
                    .gmtModify(reportAlbum.getCreateDate())
                    .modifyUser(null == reportAlbum.getCreateUserCard() ? "" : reportAlbum.getCreateUserCard())
                    .orgId(null == reportAlbum.getUserOrgId() ? "" : reportAlbum.getUserOrgId())
                    .scopeType(0)
                    .userImage(reportAlbum.getCreateHeadImgUrl())
                    .userName(reportAlbum.getCreateWxUserName())
                    .version(1)
                    .id(reportAlbum.getPkId() + "")
                    .build();
            recordMapper.insert(noteRecord);
        }


    }

    private String getId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    @Override
    public void get(Long id) {
        Order order = firstMapper.getOrder(id);
        com.example.second.domain.Order order1 = secondMapper.getOrder(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long count, Long id) {

        firstMapper.update(13L, 4L);
        firstMapper.update(15L, 2L);

        secondMapper.update(18L, 4L);
        if (count > 10) {
            throw new RuntimeException();
        }


    }


}
