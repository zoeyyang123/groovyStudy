package com.minshenglife.nbs.service.groovy

import com.minshenglife.nbs.model.FamilyInfo
import com.minshenglife.nbs.model.plan.ShouldPlan
import com.minshenglife.nbs.model.should.EducationFee

/**
 * Created by z.chen on 2018/3/1.
 * shouldPlan
 * 返回图表A的家庭应备
 * id: 10002
 */

def compute (Object object) {
    FamilyInfo familyInfo = (FamilyInfo) object
    ShouldPlan shouldPlan = new ShouldPlan()
    shouldPlan.maintenanceFee = familyInfo?.maintenanceFee
    shouldPlan.housingFee = familyInfo?.housingFee
    /**
     * familyInfo.educationFees[i].eduInfos.eduTypeSumCost由前端给出存入数据库
     * 此处直接获取
     */
    shouldPlan.educationFees = getEducationFees(familyInfo.educationFees)
    shouldPlan.supportFee = familyInfo?.supportFee
    shouldPlan.lastExpensesFee = familyInfo?.lastExpensesFee
    shouldPlan.otherLoanFee = familyInfo?.otherLoanFee
    shouldPlan.otherCostFee = familyInfo?.otherCostFee
    shouldPlan.medicalFee = familyInfo?.medicalFee
    return shouldPlan
}

def getEducationFees(List<EducationFee> educationFees) {
    if (educationFees) {
        for (int i=0; i < educationFees.size(); i++) {
            educationFees[i].totalEduFees = new BigDecimal(0.0)
            if (educationFees[i].eduInfos) {
                for (int j=0; j < educationFees[i].eduInfos.size(); j++) {
                    if (educationFees[i].eduInfos[j]?.annualEduCost
                            && educationFees[i].eduInfos[j]?.eduYear) {
                        educationFees[i].eduInfos[j].eduTypeSumCost =
                                educationFees[i].eduInfos[j].annualEduCost * educationFees[i].eduInfos[j].eduYear
                        educationFees[i].totalEduFees += educationFees[i].eduInfos[j].eduTypeSumCost
                    }
                }
            }
        }
    }
    return educationFees
}