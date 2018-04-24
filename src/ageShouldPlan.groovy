package com.minshenglife.nbs.service.groovy

import com.minshenglife.nbs.exception.GenericException
import com.minshenglife.nbs.model.FamilyInfo
import com.minshenglife.nbs.model.FamilyMembers
import com.minshenglife.nbs.model.plan.AgeMoney
import com.minshenglife.nbs.model.plan.AgeShouldPlan
import com.minshenglife.nbs.model.should.EducationFee
import com.minshenglife.nbs.model.should.EducationInfo
import com.minshenglife.nbs.model.should.HousingFee
import com.minshenglife.nbs.model.should.LastExpensesFee
import com.minshenglife.nbs.model.should.LoanInfo
import com.minshenglife.nbs.model.should.MaintenanceFee
import com.minshenglife.nbs.model.should.MedicalFee
import com.minshenglife.nbs.model.should.OtherCostFee
import com.minshenglife.nbs.model.should.OtherLoanFee
import com.minshenglife.nbs.model.should.SupportFee

/**
 * Created by z.chen on 2018/3/1.
 * ageShouldPlan
 * 返回图表C的年度应备费用
 * id: 10004
 */

def compute(Object object) {
    FamilyInfo familyInfo = (FamilyInfo) object
    AgeShouldPlan ageShouldPlan = new AgeShouldPlan()
    if (familyInfo.insured.age == null){
        throw new GenericException(50002L) //投保人年龄为空
    }
    Integer insuredAge = familyInfo.insured.age

    if (familyInfo.insured.gender == null){
        throw new GenericException(50003L) //投保人性别为空
    }

    //规划期限被保险人年龄
    if (familyInfo.planningPeriod?.planningAge == null) {
        throw new GenericException(50008L) //缺少规划期限被保险人年龄
    }
    Integer endAge = (Integer) familyInfo.planningPeriod.planningAge

    //是否包含配偶
    Boolean isSpouseInclude = familyInfo.isSpouseInclude()

    // 当前最幼子女年龄,无子女设为0
    Integer childMinAge = new Integer(0)
    if (familyInfo?.members && familyInfo.members?.children && familyInfo.members.children.size() > 0) {
        childMinAge = familyInfo.members.children[0].age
        for (int i = 1; i < familyInfo.members.children.size(); i++) {
            if (familyInfo.members.children[i].age && familyInfo.members.children[i].age < childMinAge) {
                childMinAge = familyInfo.members.children[i].age
            }
        }
    }

    //估算每年费用/收入的年龄以被保人当前年龄作为起始值
    Integer estimateAge = insuredAge
    //以下对象用于接收年龄-费用/收入/差额的数列
    List<AgeMoney> ageMaintenanceFees = new ArrayList<AgeMoney>()
    List<AgeMoney> ageHousingFundFees = new ArrayList<AgeMoney>()
    List<AgeMoney> ageHousingLoanFees = new ArrayList<AgeMoney>()
    List<AgeMoney> ageHousingRentFees = new ArrayList<AgeMoney>()
    List<AgeMoney> ageEducationFees = new ArrayList<AgeMoney>()
    List<AgeMoney> ageSupportFees = new ArrayList<AgeMoney>()
    //最后费用单列
    List<AgeMoney> ageOtherLoanFees = new ArrayList<AgeMoney>()
    List<AgeMoney> ageOtherCostFees = new ArrayList<AgeMoney>()
    List<AgeMoney> ageMedicalFees = new ArrayList<AgeMoney>()
    List<AgeMoney> ageShouldTotalFees = new ArrayList<AgeMoney>()

    while (estimateAge <= endAge) {
        /**
         * 1.生活费
         */
        BigDecimal ageMaintenanceFee = getAgeMaintenanceFee(familyInfo.maintenanceFee,
                estimateAge,insuredAge,childMinAge)
        /**
         * 2.住房费用
         */
        //公积金
        BigDecimal ageHousingFundFee = getAgeHousingFundFee(familyInfo.housingFee, estimateAge,insuredAge)
        //住房贷款
        BigDecimal ageHousingLoanFee = getAgeHousingLoanFee(familyInfo.housingFee, estimateAge,insuredAge)
        //租房
        BigDecimal ageHousingRentFee = getAgeHousingRentFee(familyInfo.housingFee, estimateAge,insuredAge)
        /**
         * 3.教育费用
         */
        BigDecimal ageEducationFee = getAgeEducationFee(familyInfo.educationFees,estimateAge,insuredAge)
        /**
         * 4.赡养费用
         */
        BigDecimal ageSupportFee = getAgeSupportFee(familyInfo.supportFee,
                familyInfo.members,estimateAge,insuredAge,isSpouseInclude)
        /**
         * 5.最后费用
         * 循环外单列
         */
        //BigDecimal ageLastExpensesFee = new BigDecimal(0)
        /**
         * 6.其他贷款
         */
        BigDecimal ageOtherLoanFee = getAgeOtherLoanFee(familyInfo.otherLoanFee,estimateAge,insuredAge)
        /**
         * 7.其他费用
         */
        BigDecimal ageOtherCostFee = getAgeOtherCostFee(familyInfo.otherCostFee,estimateAge)
        /**
         * 8.医疗费用
         */
        BigDecimal ageMedicalFee = getAgeMedicalFee(familyInfo.medicalFee,isSpouseInclude)
        /**
         * 总费用
         */
        BigDecimal ageShouldTotalFee = ageMaintenanceFee + ageHousingFundFee +
                ageHousingLoanFee + ageHousingRentFee + ageEducationFee +
                ageSupportFee + ageOtherLoanFee + ageOtherCostFee + ageMedicalFee

        //计算结果结果加入数组,当配偶存在,加入配偶对应年龄
        if (familyInfo.members && familyInfo.members.spouse && familyInfo.members.spouse.age != null) {
            Integer diffAge = familyInfo.members.spouse.age - insuredAge
            ageMaintenanceFees.add(new AgeMoney(estimateAge,estimateAge+diffAge,ageMaintenanceFee))
            ageHousingFundFees.add(new AgeMoney(estimateAge,estimateAge+diffAge,ageHousingFundFee))
            ageHousingLoanFees.add(new AgeMoney(estimateAge,estimateAge+diffAge,ageHousingLoanFee))
            ageHousingRentFees.add(new AgeMoney(estimateAge,estimateAge+diffAge,ageHousingRentFee))
            ageEducationFees.add(new AgeMoney(estimateAge,estimateAge+diffAge,ageEducationFee))
            ageSupportFees.add(new AgeMoney(estimateAge,estimateAge+diffAge,ageSupportFee))
            ageOtherLoanFees.add(new AgeMoney(estimateAge,estimateAge+diffAge,ageOtherLoanFee))
            ageOtherCostFees.add(new AgeMoney(estimateAge,estimateAge+diffAge,ageOtherCostFee))
            ageMedicalFees.add(new AgeMoney(estimateAge,estimateAge+diffAge,ageMedicalFee))
            ageShouldTotalFees.add(new AgeMoney(estimateAge,estimateAge+diffAge,ageShouldTotalFee))
        } else {
            ageMaintenanceFees.add(new AgeMoney(estimateAge,ageMaintenanceFee))
            ageHousingFundFees.add(new AgeMoney(estimateAge,ageHousingFundFee))
            ageHousingLoanFees.add(new AgeMoney(estimateAge,ageHousingLoanFee))
            ageHousingRentFees.add(new AgeMoney(estimateAge,ageHousingRentFee))
            ageEducationFees.add(new AgeMoney(estimateAge,ageEducationFee))
            ageSupportFees.add(new AgeMoney(estimateAge,ageSupportFee))
            ageOtherLoanFees.add(new AgeMoney(estimateAge,ageOtherLoanFee))
            ageOtherCostFees.add(new AgeMoney(estimateAge,ageOtherCostFee))
            ageMedicalFees.add(new AgeMoney(estimateAge,ageMedicalFee))
            ageShouldTotalFees.add(new AgeMoney(estimateAge,ageShouldTotalFee))
        }

        /**
         * 预测岁数加1,继续循环
         */
        estimateAge++
    }
    ageShouldPlan.ageMaintenanceFees = ageMaintenanceFees
    ageShouldPlan.ageHousingFundFees = ageHousingFundFees
    ageShouldPlan.ageHousingLoanFees = ageHousingLoanFees
    ageShouldPlan.ageHousingRentFees = ageHousingRentFees
    ageShouldPlan.ageEducationFees = ageEducationFees
    ageShouldPlan.ageSupportFees = ageSupportFees
    //5.最后费用
    ageShouldPlan.lastExpensesFee = getLastExpensesFee(familyInfo.lastExpensesFee,isSpouseInclude)
    ageShouldPlan.ageOtherLoanFees = ageOtherLoanFees
    ageShouldPlan.ageOtherCostFees = ageOtherCostFees
    ageShouldPlan.ageMedicalFees = ageMedicalFees
    ageShouldPlan.ageShouldTotalFees = ageShouldTotalFees

    return ageShouldPlan
}
/**
 * 计算年度生活费用
 * @param maintenanceFee
 * @param estimateAge
 * @param insuredAge
 * @param childMinAge
 * @return
 */
def getAgeMaintenanceFee(MaintenanceFee maintenanceFee, Integer estimateAge,
                         Integer insuredAge, Integer childMinAge) {
    //当生活费对象不存在，返回0
    if (maintenanceFee && maintenanceFee.monthCost != null) {
        //当最幼子女年龄为0，即无子女，返回0
        if (childMinAge == 0) {
            return maintenanceFee.monthCost * 12
        }
        //当最幼子女年龄小于等于独立年龄，即独立前
        else if (childMinAge != 0 && estimateAge - insuredAge + childMinAge <= maintenanceFee.independentAge) {
            return maintenanceFee.monthCost * 12 * maintenanceFee.insuredDeadFeePer / 100.0
        }
        //当最幼子女年龄大于独立年龄，即独立后
        else {
            return maintenanceFee.monthCost * 12 * maintenanceFee.insuredDeadFeePer / 100.0 *
                    maintenanceFee.independentFeePer / 100.0
        }
    } else {
        return new BigDecimal(0.0)
    }
}
/**
 * 计算年度公积金贷款费用
 * @param housingFee
 * @param estimateAge
 * @param insuredAge
 * @return
 */
def getAgeHousingFundFee(HousingFee housingFee, Integer estimateAge, Integer insuredAge) {
    if (housingFee && housingFee?.housingFundLoan) {
        return getAgeLoanFee(housingFee.housingFundLoan,estimateAge,insuredAge,false)
    } else {
        return new BigDecimal(0.0)
    }
}
/**
 * 计算年度商业住房贷款费用
 * @param housingFee
 * @param estimateAge
 * @param insuredAge
 * @return
 */
def getAgeHousingLoanFee(HousingFee housingFee, Integer estimateAge, Integer insuredAge) {
    if (housingFee && housingFee?.commercialLoan) {
        return getAgeLoanFee(housingFee.commercialLoan,estimateAge,insuredAge,false)
    } else {
        return new BigDecimal(0.0)
    }
}
/**
 * 计算年度租房费用
 * @param housingFee
 * @param estimateAge
 * @param insuredAge
 * @return
 */
def getAgeHousingRentFee(HousingFee housingFee, Integer estimateAge, Integer insuredAge) {
    BigDecimal rentFee = new BigDecimal(0.0)
    if (housingFee?.rentYear && housingFee?.rentMonthly && estimateAge - insuredAge < housingFee.rentYear) {
        rentFee += housingFee.rentMonthly * 12
    }
    return rentFee
}
/**
 * 年度贷款费用计算函数
 * @param loanInfo
 * @param estimateAge
 * @param insuredAge
 * @return
 */
def getAgeLoanFee(LoanInfo loanInfo, Integer estimateAge, Integer insuredAge, Boolean isRepayMonth) {
    if (isRepayMonth == false && loanInfo && loanInfo?.remainRepayAges && loanInfo?.reimbursementMonthly) {
        BigDecimal payAges = loanInfo.remainRepayAges - (estimateAge - insuredAge) //剩余应还年份
        //剩余还款年份大于0，即未完成还贷
        if (payAges > 0) {
            return loanInfo.reimbursementMonthly * 12
        } else {
            //贷款已完成，返回0
            return new BigDecimal(0.0)
        }
    } else if (isRepayMonth == true && loanInfo && loanInfo?.remainRepayMonths && loanInfo?.reimbursementMonthly) {
        BigDecimal payAges = loanInfo.remainRepayMonths / 12 - (estimateAge - insuredAge) //剩余应还年份
        //剩余还款年份大于0，即未完成还贷
        if (payAges > 0 && payAges < 1) {
            return loanInfo.reimbursementMonthly * payAges
        } else if (payAges >= 1) {
            return loanInfo.reimbursementMonthly * 12
        } else {
            //贷款已完成，返回0
            return new BigDecimal(0.0)
        }
    } else {
        //其他情况，返回0
        return new BigDecimal(0.0)
    }
}
/**
 * 计算年度教育费用
 * @param children
 * @param educationFee
 * @param estimateAge
 * @param insuredAge
 * @return
 */
def getAgeEducationFee(List<EducationFee> educationFee,
                       Integer estimateAge, Integer insuredAge) {
    BigDecimal ageEducationFee = new BigDecimal(0.0)
    if (educationFee) {
        for (int i = 0; i < educationFee.size(); i++) {
            if (educationFee[i]?.child && educationFee[i].eduInfos && educationFee[i].child?.age != null) {
                ageEducationFee += getChildAgeEduFee(educationFee[i].child.age + (estimateAge - insuredAge),
                        educationFee[i].eduInfos)
            }
        }
    }
    return ageEducationFee
}
/**
 * 获取特定年龄子女教育费用
 * @param age
 * @param eduInfos
 * @return
 */
def getChildAgeEduFee(Integer age,List<EducationInfo> eduInfos) {
    BigDecimal ageEducationFee = new BigDecimal(0.0)
    if (eduInfos) {
        for (int i = 0; i < eduInfos.size(); i++) {
            if ((eduInfos[i].eduStartAge == null && eduInfos[i].eduYear != null) ||
                    (eduInfos[i].eduStartAge != null && eduInfos[i].eduYear == null)) {
                throw new GenericException(50004L) //子女教育费用规划部分信息缺失
            }
            //假如当前年龄介于某一教育阶段 则取该阶段年度费用
            if (eduInfos[i].annualEduCost && age >= eduInfos[i].eduStartAge
                    && age < eduInfos[i].eduStartAge + eduInfos[i].eduYear) {
                ageEducationFee = eduInfos[i].annualEduCost
                break
            }
        }
    }
    return ageEducationFee
}

/**
 * 计算年度父母赡养费
 * @param supportFee
 * @param familyMembers
 * @param estimateAge
 * @param insuredAge
 * @param isSpouseInclude
 * @return
 */
def getAgeSupportFee(SupportFee supportFee, FamilyMembers familyMembers,
                     Integer estimateAge, Integer insuredAge,Boolean isSpouseInclude) {
    if ((familyMembers && familyMembers?.father && familyMembers.father?.name && familyMembers.father.age == null) ||
            (familyMembers && familyMembers?.mother && familyMembers.mother?.name && familyMembers.mother.age == null) ||
            (familyMembers && familyMembers?.fatherInLaw && familyMembers.fatherInLaw?.name && familyMembers.fatherInLaw.age == null) ||
            (familyMembers && familyMembers?.motherInLaw && familyMembers.motherInLaw?.name && familyMembers.motherInLaw.age == null)) {
        throw new GenericException(50005L) //存在父母信息但年龄为空
    }

    //男士预计死亡年龄79 女士预计死亡年龄83
    Integer fatherEstimateDeathAge = new Integer(79)
    if (familyMembers && familyMembers?.father && familyMembers.father?.estimateDeathAge != null){
        fatherEstimateDeathAge = familyMembers.father.estimateDeathAge
    }
    Integer motherEstimateDeathAge = new Integer(83)
    if (familyMembers && familyMembers?.mother && familyMembers.mother.estimateDeathAge != null){
        motherEstimateDeathAge = familyMembers.mother.estimateDeathAge
    }
    Integer fatherInLawEstimateDeathAge = new Integer(79)
    if (familyMembers && familyMembers?.fatherInLaw && familyMembers.fatherInLaw?.estimateDeathAge != null){
        fatherInLawEstimateDeathAge = familyMembers.fatherInLaw.estimateDeathAge
    }
    Integer motherInLawEstimateDeathAge = new Integer(83)
    if (familyMembers && familyMembers?.motherInLaw && familyMembers.motherInLaw?.estimateDeathAge != null){
        motherInLawEstimateDeathAge = familyMembers.motherInLaw.estimateDeathAge
    }
    //赡养费初值设为0
    BigDecimal ageParentSupport = new BigDecimal(0.0)
    //假如父母存在
    if (supportFee?.parentSupportCostMonthly && familyMembers
            && ((familyMembers.father?.age &&
            familyMembers.father?.age + estimateAge - insuredAge <= fatherEstimateDeathAge)
            || (familyMembers.mother?.age &&
            familyMembers.mother?.age + estimateAge - insuredAge <= motherEstimateDeathAge))) {
        ageParentSupport = supportFee.parentSupportCostMonthly * 12
    }
    //假如配偶父母存在
    BigDecimal ageParentInLawSupport = new BigDecimal(0.0)
    if (supportFee?.parentInLawSupportCostMonthly && familyMembers &&
            ((familyMembers.fatherInLaw?.age &&
                    familyMembers.fatherInLaw?.age + estimateAge-insuredAge <= fatherInLawEstimateDeathAge)
                    || (familyMembers.motherInLaw?.age &&
                    familyMembers.motherInLaw?.age + estimateAge - insuredAge < motherInLawEstimateDeathAge))) {
        ageParentInLawSupport = supportFee.parentInLawSupportCostMonthly * 12
    }
    if (isSpouseInclude == true) {
        //假如包含配偶 返回父母和配偶父母的赡养费之和
        return ageParentSupport + ageParentInLawSupport
    } else {
        //假如不包含配偶 返回父母赡养费
        return ageParentSupport
    }
}
/**
 * 计算最后费用
 * @param lastExpensesFee
 * @param isSpouseInclude
 * @return
 */
def getLastExpensesFee(LastExpensesFee lastExpensesFee,Boolean isSpouseInclude) {
    //被保人最后费用
    BigDecimal funeralCost = new BigDecimal(0.0)
    if (lastExpensesFee?.funeralCost) {
        funeralCost = lastExpensesFee.funeralCost
    }
    //配偶最后费用
    BigDecimal spouseFuneralCost = new BigDecimal(0.0)
    if (lastExpensesFee?.spouseFuneralCost) {
        spouseFuneralCost = lastExpensesFee.spouseFuneralCost
    }
    //是否包含配偶
    if (isSpouseInclude == true) {
        return funeralCost + spouseFuneralCost
    } else {
        return funeralCost
    }
}
/**
 * 计算年度其他贷款费用
 * @param otherLoanFee
 * @param estimateAge
 * @param insuredAge
 * @return
 */
def getAgeOtherLoanFee(OtherLoanFee otherLoanFee, Integer estimateAge, Integer insuredAge) {
    BigDecimal loanFees = new BigDecimal(0.0)
    if (otherLoanFee?.loanFees) {
        for (int i=0; i < otherLoanFee.loanFees.size(); i++) {
            loanFees += getAgeLoanFee(otherLoanFee?.loanFees[i],estimateAge,insuredAge,true)
        }
    }
    return loanFees
}
/**
 * 计算年度其他费用
 * @param otherCostFee
 * @param estimateAge
 * @return
 */
def getAgeOtherCostFee(OtherCostFee otherCostFee, Integer estimateAge) {
    BigDecimal costFees = new BigDecimal(0.0)
    if (OtherCostFee && otherCostFee?.costFees) {
        for (int i=0; i < otherCostFee.costFees.size(); i++) {
            if (estimateAge >= otherCostFee.costFees[i].startAge &&
                    estimateAge <= otherCostFee.costFees[i].endAge) {
                costFees += otherCostFee.costFees[i].annualAmount
            }
        }
    }
    return costFees
}
/**
 * 计算年度医疗费用
 * @param medicalFee
 * @param isSpouseInclude
 * @return
 */
def getAgeMedicalFee(MedicalFee medicalFee,Boolean isSpouseInclude) {
    //是否包含配偶
    if (medicalFee?.lengthOfStay && medicalFee?.wardCostDaily && isSpouseInclude == true) {
        return medicalFee.lengthOfStay * medicalFee.wardCostDaily
    } else {
        return new BigDecimal(0.0)
    }
}