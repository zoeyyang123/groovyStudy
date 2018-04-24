package com.minshenglife.nbs.service.groovy

import com.minshenglife.nbs.model.FamilyInfo
import com.minshenglife.nbs.model.plan.HavePlan

/**
 * Created by z.chen on 2018/3/1.
 * havePlan
 * 返回图表B的家庭已备
 * id: 10003
 */

def compute(Object object) {
    FamilyInfo familyInfo = (FamilyInfo) object
    HavePlan havePlan = new HavePlan()
    havePlan.income = familyInfo?.income
    havePlan.otherIncome = familyInfo?.otherIncome
    havePlan.otherAsset = familyInfo?.otherAsset
    havePlan.lifeInsurance = familyInfo?.lifeInsurance
    return havePlan
}