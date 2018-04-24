package com.minshenglife.nbs.service.groovy

import com.minshenglife.nbs.model.plan.AgeMoney
import com.minshenglife.nbs.model.plan.AgeShouldPlan

/**
 * Created by z.chen on 2018/3/1.
 * accumulativeAgeShouldPlan
 * 返回图表D的应备费用累积
 * id: 10005
 */

def compute(Object object) {
    AgeShouldPlan ageShouldPlan = (AgeShouldPlan) object
    AgeShouldPlan accumulativeAgeShouldPlan = new AgeShouldPlan()
    if (ageShouldPlan) {
        accumulativeAgeShouldPlan.ageMaintenanceFees = getAccumulative(ageShouldPlan.ageMaintenanceFees)
        accumulativeAgeShouldPlan.ageHousingFundFees = getAccumulative(ageShouldPlan.ageHousingFundFees)
        accumulativeAgeShouldPlan.ageHousingLoanFees = getAccumulative(ageShouldPlan.ageHousingLoanFees)
        accumulativeAgeShouldPlan.ageHousingRentFees = getAccumulative(ageShouldPlan.ageHousingRentFees)
        accumulativeAgeShouldPlan.ageEducationFees = getAccumulative(ageShouldPlan.ageEducationFees)
        accumulativeAgeShouldPlan.ageSupportFees = getAccumulative(ageShouldPlan.ageSupportFees)
        accumulativeAgeShouldPlan.ageOtherLoanFees = getAccumulative(ageShouldPlan.ageOtherLoanFees)
        accumulativeAgeShouldPlan.ageOtherCostFees = getAccumulative(ageShouldPlan.ageOtherCostFees)
        accumulativeAgeShouldPlan.ageMedicalFees = getAccumulative(ageShouldPlan.ageMedicalFees)
        accumulativeAgeShouldPlan.ageShouldTotalFees = getAccumulative(ageShouldPlan.ageShouldTotalFees)
    }
    //最后费用单列
    if (ageShouldPlan?.lastExpensesFee) {
        accumulativeAgeShouldPlan.lastExpensesFee = ageShouldPlan.lastExpensesFee
    }
    return accumulativeAgeShouldPlan
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