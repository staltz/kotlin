fun test() {

    fun local(){
        {
            //static instance access
            local()
        }()
    }

    //static instance access
    {
        //static instance access
        local()
    }()

    //static instance access
    (::local)()
}

// 3 GETSTATIC ConstClosureOptimization\$test\$1\.INSTANCE\$
// 1 GETSTATIC ConstClosureOptimization\$test\$2\.INSTANCE\$
// 1 GETSTATIC ConstClosureOptimization\$test\$3\.INSTANCE\$
