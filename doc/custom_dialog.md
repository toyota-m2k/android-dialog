# UtDialog クラスを継承してカスタムダイアログを作る

UtDialog は、タイトルバー（leftButton, titleView, rightButton)と、ダイアログボディ(bodyContainer, bodyView) から構成されており、新たにダイアログを作成する場合は、最低限、UtDialogを派生するクラスを作成し、bodyView を作成して返す createBodyView()メソッドをオーバーライドする。

以下、ダイアログのサイズ（高さ）をどう扱うかを中心に、いくつかのサンプルを挙げて説明する。
尚、これらのサンプルは、UtDialog派生クラスの作り方を説明するものであり、その目的以外の実装、デバイス回転時などの状態復元などは含んでいない。

## 【サンプル１】 最も簡単な例：高さ固定の小さいダイアログ

これは「名前」を入力させるEditTextが１つだけの、とても小さなダイアログで、HeightOption.COMPACT を使ったダイアログの実装例である。
このように、PhoneのLandscapeに必ず収まる程度の高さの場合は HeightOption.COMPACT を使用する。

このオプションを使用する場合、bodyViewのルートViewGroupの layout_height は、必ず、wrap_content または、固定値（dp単位など）を指定する。

尚、ダイアログの幅は、LIMIT (setLimitWidth)を使い、「400pxを最大として画面幅に合わせる」設定としている。Phoneを考慮したダイアログとする場合、常にこの設定だけで事足りるだろう。

また、入力値の「名前」を呼び出し元に返すため、onPositive()をオーバーライドして、nameプロパティにセットする実装も示している。
（※このあとのサンプル２以降は、値を返す実装は省略している。）


`CompactDialog.kt`
```Kotlin
class CompactDialog : UtDialog() {
    init {
        title="小さいダイアログ"
        setLimitWidth(400)
        heightOption=HeightOption.COMPACT
        setLeftButton(BuiltInButtonType.CANCEL)
        setRightButton(BuiltInButtonType.DONE)
    }

    // 呼び出し元から、結果（このダイアログだと入力された名前）を取り出せるようにするためのプロパティ
    var name:String? = null

    /**
     * ダイアログの中身 (bodyView)を作成して返す。
     */
    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        return inflater.inflate(R.layout.sample_compact_dialog)
    }

    /**
     * Doneボタンがタップされたときの処理
     * 呼び出し元から結果が参照できるように、入力された内容をプロパティとして取り出しておく。
     */
    override fun onPositive() {
        name = dialog?.findViewById<EditText>(R.id.name_input)?.text?.toString() ?: ""
        super.onPositive()
    }
}
```

`sample_compact_dialog.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical">

    <TextView
        android:id="@+id/name_label"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Name"
        />
    <EditText
        android:id="@+id/name_input"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        />
</LinearLayout>
```

## 【サンプル２】 中身の高さに合わせて伸縮し、必要ならスクロールする

Prefernce設定など、たくさんの設定項目を縦に並べるダイアログの場合は、HeightOption.AUTO_SCROLL を使用する。このオプションを使うと、bodyViewの高さが小さいとき（＝画面内に収まるとき）は、そのサイズにあわせてダイアログの高さが調整され、bodyViewの高さが大きくなり、ダイアログの高さを画面いっぱいに拡大しても収まらなくなれば、bodyViewがスクロールするようになる。

HeightOption.AUTO_SCROLL を使用する場合、bodyViewのルートViewGroupの layout_height は、必ず、wrap_content を指定する。

このサンプルでは、"Add Item"/"Delete Item" ボタンで、bodyViewにアイテム(TextView)が追加・削除され、それに合わせて、bodyViewのサイズが変化する。このとき、画面高さに余裕がある間は、ダイアログの高さが伸び、画面高さいっぱいになると、そこで高さの拡大は止まって、bodyView全体がスクロールし始める動作が確認できる。

ちなみに、アイテムの追加・削除のコードを除くと、サンプル１との本質的な違いは、heightOptionだけであり、実際、サンプル１で、AUTO_SCROLL を指定しても、高さが画面高さを超える状況が起こり得ないだけで、まったく正しく動作する。

`AutoScrollDialog.kt`
```Kotlin
class AutoScrollDialog : UtDialog() {
    init {
        title="Auto Scroll Test"
        setLimitWidth(400)
        heightOption=HeightOption.AUTO_SCROLL
        setLeftButton(BuiltInButtonType.CANCEL)
        setRightButton(BuiltInButtonType.DONE)
    }

    var count:Int = 0
    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        return inflater.inflate(R.layout.sample_auto_scroll_dialog).apply {
            // Add Item ボタンタップで、アイテムを追加
            findViewById<Button>(R.id.add_item_button).setOnClickListener {
                val view = TextView(requireContext()).apply {
                    count++
                    text = "Item $count"
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                }
                (bodyView as LinearLayout).addView(view)
            }
            // Delete Item ボタンタップで、アイテムを削除
            findViewById<Button>(R.id.del_item_button).setOnClickListener {
                val view = (bodyView as LinearLayout).children.last()
                if(view is TextView) {
                    (bodyView as LinearLayout).removeView(view)
                }
            }
        }
    }
}
```

`sample_auto_scroll_dialog.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    >

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        >
        <Button
            android:id="@+id/add_item_button"
            android:layout_width="150dp"
            android:layout_height="wrap_content"
            android:text="add item"
            />
        <Space
            android:layout_width="10dp"
            android:layout_height="0dp"/>
        <Button
            android:id="@+id/del_item_button"
            android:layout_width="150dp"
            android:layout_height="wrap_content"
            android:text="Delete item"
            />

    </LinearLayout>

</LinearLayout>
```

## 【サンプル３】 高さ固定のダイアログ ～ 伸縮・スクロールするビューを持つ場合

ダイアログボディ内に、ListViewやRecycleView, あるいは、自前のScrollView などを持つ場合、HeightOption.AUTO_SCROLL は使えない。
このような場合は、HeightOption.FILL または、CUSTOM を使用する。このサンプルは、bodyView内に、ListViewを持つダイアログで、HeightOption.FILL を使う場合の実装を示している。

このオプションを使用する場合は、bodyViewのルートViewGroupの layout_height には、MATCH_PARENT を指定する。
また、ListViewの高さには、`layout_height="0dp" layout_weight="1"` を指定している。(余談だが、ルートViewGroupに ConstraintLayoutを使用する場合は、`layout_weight`の代わりに、layout_top*, layout_bottom* でうまい具合に指定することはいうまでもない）。

このサンプルでは、最初から画面の高さいっぱいのダイアログが表示され、ListViewが、そのダイアログ内に表示可能な最大サイズで配置されることが確認できる。

`FillDialog.kt`
```Kotlin
class FillDialog : UtDialog() {
    init {
        title="Auto Scroll Test"
        setLimitWidth(400)
        heightOption=HeightOption.FULL
        setLeftButton(BuiltInButtonType.CANCEL)
        setRightButton(BuiltInButtonType.DONE)
    }

    private inner class ListAdapter: BaseAdapter() {
        val items:MutableList<String> = mutableListOf()

        override fun getCount(): Int = items.size
        override fun getItem(position: Int): Any = items[position]
        override fun getItemId(position: Int): Long = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            return (convertView as? TextView ?: TextView(requireContext())).also { it.text = items[position] }
        }
        fun add(item:String) {
            items.add(item)
            notifyDataSetChanged()
        }
        fun remove() {
            if(items.size==0) return
            items.removeLast()
            notifyDataSetChanged()
        }
    }

    var count:Int = 0

    lateinit var listView: ListView
    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        return inflater.inflate(R.layout.sample_fill_dialog).apply {
            listView = findViewById(R.id.list_view)
            listView.adapter = ListAdapter()

            findViewById<Button>(R.id.add_item_button).setOnClickListener {
                count++
                (listView.adapter as ListAdapter).add("Item - $count")
            }
            findViewById<Button>(R.id.del_item_button).setOnClickListener {
                (listView.adapter as ListAdapter).remove()
            }
        }
    }
}
```

`sample_fill_dialog.kt`
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    >

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        >
        <Button
            android:id="@+id/add_item_button"
            android:layout_width="150dp"
            android:layout_height="wrap_content"
            android:text="add item"
            />
        <Space
            android:layout_width="10dp"
            android:layout_height="0dp"/>
        <Button
            android:id="@+id/del_item_button"
            android:layout_width="150dp"
            android:layout_height="wrap_content"
            android:text="Delete item"
            />

    </LinearLayout>

    <ListView
        android:id="@+id/list_view"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:background="@color/white"
        />

</LinearLayout>
```

## 【サンプル４】 高さ可変のダイアログ ～ 伸縮・スクロールするビューを持つ場合

サンプル３では、ListViewを常に最大サイズで表示したが、
「通常は、たかだか数個のアイテムを表示するだけ、まれに、スクロールするほどのアイテムを表示することもある」という場合には、ListViewをできるだけ小さく（中身が表示できる最小高さで）配置したくなる。このような場合は、少しコード量は増えるが、HeightOption.CUSTOM を指定し、calcCustomContainerHeight()をオーバーライドすることにより、AUTO_SCROLL の動作に似た、ListViewの中身にあわせて伸縮するダイアログが実現できる。尚、リストの高さが変化するとき（アイテムが増減するとき）に、updateCustomHeight()を呼んで再配置が必要であることを、UtDialogに伝えることを忘れずに。

※サンプル３と同じレイアウト(sample_fill_dialog.xml)を使用。

`CustomDialog.kt`
```Kotlin
class CustomDialog : UtDialog() {
    init {
        title="Auto Scroll Test"
        setLimitWidth(400)
        heightOption=HeightOption.CUSTOM
        setLeftButton(BuiltInButtonType.CANCEL)
        setRightButton(BuiltInButtonType.DONE)
    }

    private inner class ListAdapter:BaseAdapter() {
        val items:MutableList<String> = mutableListOf()

        override fun getCount(): Int = items.size
        override fun getItem(position: Int): Any = items[position]
        override fun getItemId(position: Int): Long = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            return (convertView as? TextView ?: TextView(requireContext())).also { it.text = items[position] }
        }
        fun add(item:String) {
            items.add(item)
            notifyDataSetChanged()
        }
        fun remove() {
            if(items.size==0) return
            items.removeLast()
            notifyDataSetChanged()
        }
    }

    var count:Int = 0

    lateinit var listView: ListView
    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        return inflater.inflate(R.layout.sample_fill_dialog).apply {
            listView = findViewById(R.id.list_view)
            listView.adapter = ListAdapter()

            findViewById<Button>(R.id.add_item_button).setOnClickListener {
                count++
                (listView.adapter as ListAdapter).add("Item - $count")
                updateCustomHeight()
            }
            findViewById<Button>(R.id.del_item_button).setOnClickListener {
                (listView.adapter as ListAdapter).remove()
                updateCustomHeight()
            }
        }
    }

    /**
     * リストビュー内のアイテムが少ないときは、できるだけコンパクトに、
     * アイテムが増えてきたら、画面いっぱいまで拡張し、それ以降はスクロールさせる。
     */
    override fun calcCustomContainerHeight(
        currentBodyHeight: Int,
        currentContainerHeight: Int,
        maxContainerHeight: Int
    ): Int {
        val calculatedLvHeight = listView.calcFixedContentHeight()
        val remainHeight = currentBodyHeight-listView.height    // == listviewを除く、その他のパーツの高さ合計
        val maxLvHeight = maxContainerHeight - remainHeight     // listViewの最大高さ
        return if(calculatedLvHeight>=maxLvHeight) {
            // リストビューの中身が、最大高さを越える --> 最大高さを採用
            listView.setLayoutHeight(maxLvHeight)
            maxContainerHeight
        } else {
            // リストビューの中身が、最大高さより小さい --> リストビューの中身のサイズを採用
            listView.setLayoutHeight(calculatedLvHeight)
            calculatedLvHeight + remainHeight
        }
    }

    // ユーティリティ：リストビューのコンテントの高さを計算
    fun ListView.calcFixedContentHeight():Int {
        val listAdapter = adapter ?: return 0
        if(count==0) return 0
        val listItem = listAdapter.getView(0, null, this)
        listItem.measure(0, 0)
        val itemHeight = listItem.measuredHeight
        return itemHeight * count + dividerHeight * (count-1)
    }
}
```