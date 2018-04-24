package com.minshenglife.nbs.service.groovy

import com.minshenglife.nbs.exception.GenericException
import com.minshenglife.nbs.model.FinancialPlan
import com.minshenglife.nbs.model.plan.AgeMoney
import com.minshenglife.nbs.model.plan.DiffShouldHave

/**
 * Created by z.chen on 2018/3/1.
 * diffAccumulativeAgeShouldHave
 * 返回图表F的累积费用差额
 * id: 10008
 */

def compute(Object object) {
    FinancialPlan financialPlan = (FinancialPlan) object
    DiffShouldHave diffAccumulativeAgeShouldHave = new DiffShouldHave()
    diffAccumulativeAgeShouldHave.ageShould = financialPlan.accumulativeAgeShouldPlan.ageShouldTotalFees
    diffAccumulativeAgeShouldHave.ageHave = financialPlan.accumulativeAgeHavePlan.ageHaveTotalMoney
    diffAccumulativeAgeShouldHave.ageDiffShouldHave =
            getDiff(diffAccumulativeAgeShouldHave.ageShould,diffAccumulativeAgeShouldHave.ageHave)
    return diffAccumulativeAgeShouldHave
}

def getDiff (List<AgeMoney> should, List<AgeMoney> have) {
    List<AgeMoney> diffShouldHave = new ArrayList<AgeMoney>()
    if (should && have && should.size() == have.size()) {
        for (int i=0; i < should.size(); i++) {
            if (should[i].spouseAge != null && have[i].spouseAge != null) {
                diffShouldHave.add(new AgeMoney(should[i].age,should[i].spouseAge,should[i].money-have[i].money))
            } else {
                diffShouldHave.add(new AgeMoney(should[i].age,should[i].money-have[i].money))
            }
        }
    } else {
        throw new GenericException(50007L) //应备或已备数据缺失
    }
    return diffShouldHave
}