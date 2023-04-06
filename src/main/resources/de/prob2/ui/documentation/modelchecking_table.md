
#foreach($machine in $machines)
#Modelchecking Table of $util.latexSafe($machine.getName())
=================================
| Modelchecking Task | Modelchecking Result |
| ------------------ | -------------------- |
#foreach($item in $machine.getModelcheckingItems())
	#if($item.selected())
		#if($item.getItems().isEmpty())
| $util.modelcheckingToUString($item,$i18n) | $i18n.translate("verifications.result.notChecked.header") |
		#else
			#foreach($result in $item.getItems())
| $util.modelcheckingToUIString($item,$i18n) | $result.getMessage() |
			#end
		#end
	#end
#end
#end
