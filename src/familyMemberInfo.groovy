package com.minshenglife.nbs.service.groovy

import com.minshenglife.nbs.model.FamilyInfo
import com.minshenglife.nbs.model.plan.FamilyMemberInfo

/**
 * Created by z.chen on 2018/3/1.
 * familyMemberInfo
 * 返回图表A的家庭信息
 * id: 10001
 */

def compute(Object object) {
    FamilyInfo familyInfo = (FamilyInfo) object
    FamilyMemberInfo familyMemberInfo = new FamilyMemberInfo()
    familyMemberInfo.insured = familyInfo?.insured
    familyMemberInfo.familyMembers = familyInfo?.members
    return familyMemberInfo
}