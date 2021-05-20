# HeightOption

ダイアログの高さの決定方法を指定するフラグ

|値|意味|bodyViewのlayout_height|備考|
|:---:|:---:|:---:|:---:|
|COMPACT|bodyViewのサイズに合わせる|WRAP_CONTENTまたは、dp値||
|FULL|フルスクリーン表示する|MATCH_PARENT||
|FIXED|固定値|MATCH_PARENT|固定値(dp)をheightHintプロパティで指定する.|
|AUTO_SCROLL|フルスクリーンを最大値として、コンテントが収まる高さに自動調整。収まらない場合はスクロールする.|MATCH_PARENT|scrollable=trueが自動設定される.|
|CUSTOM|AUTO_SCROLL 的な配置をサブクラスで実装する.|MATCH_PARENT|scrollable=trueが自動設定される.|


## Note

- bodyViewの高さに応じて、bodyView全体を自動的にスクロールさせる場合は、AUTO_SCROLL が便利。
- ただし、bodyView内に、ListViewやRecycleViewなど、スクロール可能なコントロールを含む場合は、FULL を指定して、スクロール可能なコントロールを自動伸縮するようにする。
- スクロール可能なコントロールを持っているが、通常の項目数が少なく、余白が目立ってブサイクな場合は、CUSTOM を指定して、自力で高さを調節することも出来る。
  