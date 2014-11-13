 JAVA_HOME/bin/javac   $pro/src/ict/edu/learning/test/Test.java
echo 'compiling is over'
nohup $JAVA_HOME/bin/java ict/edu/learning/test/Test -train $pro/data/\
OHSUMED/OHSUMED/QueryLevelNorm/Fold1/train.txt  -test  $pro/data/OHSUMED/OHSUMED/QueryLevelNorm/Fold1/test.txt \
-validate   $pro/data/OHSUMED/OHSUMED/QueryLevelNorm/Fold1/vali.txt -nThread 17 -norm zscore -learningRate 0.00000001 &
echo 'It is done'

