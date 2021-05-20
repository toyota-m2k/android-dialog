# WidthOption

ダイアログ幅の決定方法を指定するフラグ

|値|意味|bodyViewのlayout_width|備考|
|:---:|:---:|:---:|:---:|
|COMPACT|bodyViewのサイズに合わせる|WRAP_CONTENTまたは、dp値||
|FULL|フルスクリーン表示する|MATCH_PARENT||
|FIXED|固定値|MATCH_PARENT|固定値(dp)をwidthHintプロパティで指定する|
|LIMIT|フルスクリーンを最大値としてwidthHintを越えない幅に自動調整する|MATCH_PARENT|最大幅(dp)をwidthHintプロパティで指定する|


## Note
- Phone縦置きでは全画面表示、Tabletでは、ちょうどいいサイズとしたい、という場合は、LIMITを選択し、widthHint = 400dp のように指定するのが便利。
