// IMyAidlInterface.aidl
package cn.thens.okbinder.sample;

// Declare any non-default types here with import statements

interface IMyAidlInterface {

    IMyAidlInterface testCallback(in IMyAidlInterface callback);
}
