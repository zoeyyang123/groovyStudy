package com.minshenglife.nbs.service.groovy

import com.minshenglife.nbs.exception.GenericException
import com.minshenglife.nbs.model.FamilyInfo
import com.minshenglife.nbs.model.FamilyMembers
import com.minshenglife.nbs.model.enums.DiffGraphInsuranceType
import com.minshenglife.nbs.model.enums.Gender
import com.minshenglife.nbs.model.enums.InsuranceType
import com.minshenglife.nbs.model.have.Income
import com.minshenglife.nbs.model.have.LifeInsurance
import com.minshenglife.nbs.model.have.LifeInsuranceInfo
import com.minshenglife.nbs.model.have.OtherAsset
import com.minshenglife.nbs.model.have.OtherIncome
import com.minshenglife.nbs.model.plan.AgeHavePlan
import com.minshenglife.nbs.model.plan.AgeMoney

/**
 * Created by z.chen on 2018/3/1.
 * ageHavePlan
 * 返回年度已备费用
 * id: 10006
 */

def compute(Object object) {
    FamilyInfo familyInfo = (FamilyInfo) object

    if (familyInfo?.insured?.age == null){
        throw new GenericException(50002L) //投保人年龄为空
    }
    Integer insuredAge = familyInfo.insured.age

    if (familyInfo?.insured?.gender == null){
        throw new GenericException(50003L) //投保人性别为空
    }
    String insuredGender = familyInfo.insured.gender
    //预计死亡年龄
    Integer insuredEstimateDeathAge = familyInfo.insured?.estimateDeathAge
    if (!familyInfo?.insured?.estimateDeathAge && insuredGender == Gender.MALE.name()) {
        insuredEstimateDeathAge = 79
    } else if (!familyInfo?.insured?.estimateDeathAge && insuredGender == Gender.FEMALE.name()) {
        insuredEstimateDeathAge = 83
    }
    Integer spouseEstimateDeathAge = new Integer(0)
    if (familyInfo?.members?.spouse) {
        spouseEstimateDeathAge = familyInfo.members.spouse?.estimateDeathAge
        if (!familyInfo?.members?.spouse?.estimateDeathAge && insuredGender == Gender.MALE.name()) {
            spouseEstimateDeathAge = 83
        } else if (!familyInfo?.members?.spouse?.estimateDeathAge && insuredGender == Gender.FEMALE.name()) {
            spouseEstimateDeathAge = 79
        }
    }
    //是否包含配偶
    Boolean isSpouseInclude = familyInfo.isSpouseInclude()
    //差额图类型
    String diffGraphInsuranceType = familyInfo.diffGraphInsuranceType
    //规划期限被保险人年龄
    if (familyInfo.planningPeriod?.planningAge == null) {
        throw new GenericException(50008L) //缺少规划期限被保险人年龄
    }
    Integer endAge = familyInfo.planningPeriod.planningAge
    //估算每年费用/收入的年龄以被保人当前年龄作为起始值
    Integer estimateAge = insuredAge

    AgeHavePlan ageHavePlan = new AgeHavePlan()
    List<AgeMoney> ageIncomes = new ArrayList<AgeMoney>()
    List<AgeMoney> ageOtherIncomes = new ArrayList<AgeMoney>()
    List<AgeMoney> ageOtherAssets = new ArrayList<AgeMoney>()
    List<AgeMoney> ageLifeInsurances = new ArrayList<AgeMoney>()
    List<AgeMoney> ageHaveTotalMoneys = new ArrayList<AgeMoney>()

    while (estimateAge <= endAge) {
        /**
         * 1.年度收入/配偶年收入
         */
        BigDecimal ageIncome = getAgeIncome(familyInfo.income,estimateAge,
                insuredAge,familyInfo.members,isSpouseInclude)
        /**
         * 2.年度其他收入
         */
        BigDecimal ageOtherIncome = getAgeOtherIncome(familyInfo.otherIncome,estimateAge)
        /**
         * 3.年度资产
         * 资产*收益率
         */
        BigDecimal ageOtherAsset = getAgeOtherAsset(familyInfo.otherAsset)
        /**
         * 4.年度保险收入
         */
        BigDecimal ageLifeInsurance = getAgeLifeInsurance(familyInfo.lifeInsurance,
                estimateAge,insuredEstimateDeathAge,diffGraphInsuranceType)
        /**
         * 年度总收入
         */
        BigDecimal ageHaveTotalMoney = ageIncome + ageOtherIncome + ageOtherAsset + ageLifeInsurance

        //计算结果加入数组,当配偶存在,加入配偶对应年龄
        if (familyInfo?.members?.spouse?.age != null) {
            Integer diffAge = familyInfo.members.spouse.age - insuredAge
            ageIncomes.add(new AgeMoney(estimateAge,estimateAge+diffAge,ageIncome))
            ageOtherIncomes.add(new AgeMoney(estimateAge,estimateAge+diffAge,ageOtherIncome))
            ageOtherAssets.add(new AgeMoney(estimateAge,estimateAge+diffAge,ageOtherAsset))
            ageLifeInsurances.add(new AgeMoney(estimateAge,estimateAge+diffAge,ageLifeInsurance))
            ageHaveTotalMoneys.add(new AgeMoney(estimateAge,estimateAge+diffAge,ageHaveTotalMoney))
        } else {
            ageIncomes.add(new AgeMoney(estimateAge,ageIncome))
            ageOtherIncomes.add(new AgeMoney(estimateAge,ageOtherIncome))
            ageOtherAssets.add(new AgeMoney(estimateAge,ageOtherAsset))
            ageLifeInsurances.add(new AgeMoney(estimateAge,ageLifeInsurance))
            ageHaveTotalMoneys.add(new AgeMoney(estimateAge,ageHaveTotalMoney))
        }

        /**
         * estimateAge 自增循环
         */
        estimateAge++
    }
    ageHavePlan.ageIncome = ageIncomes
    ageHavePlan.ageOtherIncome = ageOtherIncomes
    ageHavePlan.ageOtherAsset = ageOtherAssets
    ageHavePlan.ageLifeInsurance = ageLifeInsurances
    ageHavePlan.ageHaveTotalMoney = ageHaveTotalMoneys
    return ageHavePlan
}
/**
 * 年度配偶收入
 * @param income
 * @param estimateAge
 * @param insuredAge
 * @param spouseAge
 * @param isSpouseInclude
 * @return
 */
def getAgeIncome(Income income, Integer estimateAge, Integer insuredAge, FamilyMembers familyMembers, Boolean isSpouseInclude) {
    Integer spouseAge = -1
    if (familyMembers?.spouse && familyMembers.spouse?.age != null) {
        spouseAge = familyMembers.spouse.age
    }
    BigDecimal spouseIncome = new BigDecimal(0.0)
    if (spouseAge != -1 && income?.spouseRetiredAge != null && income?.spouseAnnualIncome != null
            && estimateAge - insuredAge + spouseAge <= income.spouseRetiredAge) {
        spouseIncome = income.spouseAnnualIncome
    }
    //是否包含配偶
    if (isSpouseInclude == true) {
        //包含配偶则为配偶收入
        return spouseIncome
    } else {
        //不包含配偶则为0
        return new BigDecimal(0.0)
    }
}
/**
 * 年度其他费用
 * @param otherIncome
 * @param estimateAge
 * @return
 */
def getAgeOtherIncome(OtherIncome otherIncome, Integer estimateAge) {
    BigDecimal ageOtherIncome = new BigDecimal(0.0)
    if (otherIncome?.otherIncomeList) {
        for (int i=0; i < otherIncome.otherIncomeList.size(); i++) {
            if (otherIncome.otherIncomeList[i]?.incomeStartAge
                    && otherIncome.otherIncomeList[i]?.incomeEndAge
                    && otherIncome.otherIncomeList[i]?.incomeAmountMonthly
                    && estimateAge >= otherIncome.otherIncomeList[i].incomeStartAge
                    && estimateAge <= otherIncome.otherIncomeList[i].incomeEndAge) {
                ageOtherIncome += otherIncome.otherIncomeList[i].incomeAmountMonthly * 12
            }
        }
    }
    return ageOtherIncome
}
/**
 * 年度其他资产收益
 * @param otherAsset
 * @return
 */
def getAgeOtherAsset(OtherAsset otherAsset) {
    BigDecimal ageOtherAsset = new BigDecimal(0.0)
    if (otherAsset?.otherAssetList) {
        for (int i=0; i < otherAsset.otherAssetList.size(); i++) {
            if (otherAsset.otherAssetList[i]?.assetAmount &&
                    otherAsset.otherAssetList[i]?.assetYield) {
                ageOtherAsset += otherAsset.otherAssetList[i].assetAmount *
                        otherAsset.otherAssetList[i].assetYield / 100
            }
        }
    }
    return ageOtherAsset.setScale(2,BigDecimal.ROUND_HALF_UP)
}
/**
 * 年度被保人保险
 * @param lifeInsurance
 * @param estimateAge
 * @param insuredEstimateDeathAge
 * @param spouseEstimateDeathAge
 * @return
 */
def getAgeLifeInsurance(LifeInsurance lifeInsurance, Integer estimateAge,
                        Integer insuredEstimateDeathAge,String diffGraphInsuranceType) {
    BigDecimal ageLifeInsurance = new BigDecimal(0.0)
    if (lifeInsurance?.insuredLifeInsuranceList) {
        for (int i=0; i < lifeInsurance.insuredLifeInsuranceList.size(); i++) {
            //假如被保人预计死亡年龄小于满期年龄 则预计死亡年龄时领取保险金
            //即预测年龄等于死亡年龄
            if (lifeInsurance.insuredLifeInsuranceList[i]?.expirationAge
                    && insuredEstimateDeathAge <= lifeInsurance.insuredLifeInsuranceList[i].expirationAge &&
                    estimateAge == insuredEstimateDeathAge){
                ageLifeInsurance += getInsuranceAmount(lifeInsurance.insuredLifeInsuranceList[i],diffGraphInsuranceType)
            }
            //假如被保人预计死亡年龄大于满期年龄 则满期时领取保险金
            //即预测年龄等于满期年龄
            else if (lifeInsurance.insuredLifeInsuranceList[i]?.expirationAge
                    && insuredEstimateDeathAge > lifeInsurance.insuredLifeInsuranceList[i].expirationAge &&
                    estimateAge == lifeInsurance.insuredLifeInsuranceList[i].expirationAge){
                ageLifeInsurance += getInsuranceAmount(lifeInsurance.insuredLifeInsuranceList[i],diffGraphInsuranceType)
            }
            //其它情况为0
        }
    }
    return ageLifeInsurance
}
/**
 * 根据险种类型和差额图类型进行判断
 * @param lifeInsuranceInfo
 * @param diffGraphInsuranceType
 * @return
 */
def getInsuranceAmount(LifeInsuranceInfo lifeInsuranceInfo,String diffGraphInsuranceType){
    //隐藏意外险和重疾险
    if (diffGraphInsuranceType == DiffGraphInsuranceType.HIDEACCIDENTANDILL.name() &&
            (lifeInsuranceInfo?.insuranceType == InsuranceType.TERMACCIDENTINSURANCE.name() ||
                    lifeInsuranceInfo?.insuranceType == InsuranceType.TERMCRITICILLINSURANCE.name() ||
                    lifeInsuranceInfo?.insuranceType == InsuranceType.WHOLECRITICILLINSURANCE.name())){
        return new BigDecimal(0.0)
    }
    //隐藏意外险
    else if (diffGraphInsuranceType == DiffGraphInsuranceType.HIDEACCIDENTINSURANCE.name() &&
            lifeInsuranceInfo?.insuranceType == InsuranceType.TERMACCIDENTINSURANCE.name()) {
        return new BigDecimal(0.0)
    }
    //隐藏重疾险
    else if (diffGraphInsuranceType == DiffGraphInsuranceType.HIDECRITICILLINSURANCE.name() &&
            (lifeInsuranceInfo?.insuranceType == InsuranceType.TERMCRITICILLINSURANCE.name() ||
                    lifeInsuranceInfo?.insuranceType == InsuranceType.WHOLECRITICILLINSURANCE.name())) {
        return new BigDecimal(0.0)
    }
    //其他情况直接返回保额
    else {
        if (lifeInsuranceInfo?.amountCovered) {
            return lifeInsuranceInfo.amountCovered
        } else {
            return new BigDecimal(0.0)
        }
    }
}