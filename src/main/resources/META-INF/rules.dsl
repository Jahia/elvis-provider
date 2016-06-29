[condition][]- the property match the elvis condition=eval(elvisRulesService.checkCondition(property))
[condition][]- the node was using an elvis file=eval(elvisRulesService.checkCondition($node.getIdentifier()))
[consequence][]Write asset usage in elvis=elvisRulesService.writeUsageInElvis(property);
[consequence][]Remove asset usage in elvis=elvisRulesService.removeUsageInElvis(node.getIdentifier());
