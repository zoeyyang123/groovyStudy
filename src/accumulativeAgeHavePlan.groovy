package com.minshenglife.nbs.service.groovy

import com.minshenglife.nbs.model.plan.AgeHavePlan
import com.minshenglife.nbs.model.plan.AgeMoney

/**
 * Created by z.chen on 2018/3/1.
 * accumulativeAgeHavePlan
 * 返回图表E的已备费用累积
 * id: 10007
 */

def compute(Object object) {
    AgeHavePlan  ageHavePlan = (AgeHavePlan) object
    AgeHavePlan accumulativeAgeHavePlan = new AgeHavePlan()

    if (ageHavePlan) {
        accumulativeAgeHavePlan.ageIncome = getAccumulative(ageHavePlan.ageIncome)
        accumulativeAgeHavePlan.ageOtherIncome = getAccumulative(ageHavePlan.ageOtherIncome)
        accumulativeAgeHavePlan.ageOtherAsset = getAccumulative(ageHavePlan.ageOtherAsset)
        accumulativeAgeHavePlan.ageLifeInsurance = getAccumulative(ageHavePlan.ageLifeInsurance)
        accumulativeAgeHavePlan.ageHaveTotalMoney = getAccumulative(ageHavePlan.ageHaveTotalMoney)
    }
    return accumulativeAgeHavePlan
}

def getAccumulative(List<AgeMoney> ageMoneyList) {
    Integer listSize = ageMoneyList.size()
    List<AgeMoney> accumulativeAgeMoney = new ArrayList<AgeMoney>()
    List<AgeMoney> reversedAgeMoney = new ArrayList<AgeMoney>()
    BigDecimal accumulativeMoney = new BigDecimal(0)
    // reversedAgeMoney为从期限年龄到当前年龄排序
    //判断AgeMoney对象是否有配偶年龄
    if (ageMoneyList?.spouseAge) {
        for (int i=0; i < listSize; i++) {
            accumulativeMoney += ageMoneyList[listSize-1-i].money
            reversedAgeMoney.add(new AgeMoney(ageMoneyList[listSize-1-i].age,
                    ageMoneyList[listSize-1-i].spouseAge,accumulativeMoney))
        }
    } else {
        for (int i=0; i < listSize; i++) {
            accumulativeMoney += ageMoneyList[listSize-1-i].money
            reversedAgeMoney.add(new AgeMoney(ageMoneyList[listSize-1-i].age,accumulativeMoney))
        }
    }
    // 输出List时调整为当前年龄到期限年龄
    for (int i=0; i < listSize; i++) {
        accumulativeAgeMoney.add(reversedAgeMoney[listSize-1-i])
    }
    return accumulativeAgeMoney
}